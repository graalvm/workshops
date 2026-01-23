# From JIT to Native: Efficient Java Containers with GraalVM and Spring Boot

This workshop demonstrates how to **build efficient, size-optimized native applications** with [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) and Spring Boot, and deploy them in various containers to optimize the runtime environment.

You are going to discover ways to minimize application footprint by taking advantage of different Native Image linking options, and packaging a size-compact application into various containers, focusing on two main strategies: distroless and static, discussing the trade-offs.

Each step in this workshop is implemented as a multistage build.
It uses the [Oracle GraalVM container image](https://container-registry.oracle.com/ords/ocr/ba/graalvm) as the builder and explores different base images for the runner.
Each step is automated via scripts and Dockerfiles.

For the demo part, you will run a Spring Boot web server application, hosting the GraalVM website latest release documentation.
Spring Boot 3 has integrated support for GraalVM Native Image, making it easier to set up and configure a project.
Compiling a Spring Boot application ahead of time can significantly improve the performance and reduce its footprint.

### Objectives

In this workshop you will:

- Deploy the application in different containers to optimize the runtime environment.
- Compile this Spring Boot application ahead of time into a native image using either GraalVM or Paketo Buildpacks.
- Learn about different file size optimization options that Native Image offers.
- See how to apply the [Maven plugin for Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).
- Reduce containers size by taking advantage of different Native Image static linking options.
- Compare the deployed container images sizes and discuss the trade offs, focusing on two main strategies: distroless and static.

### Prerequisites

* x86 Linux
* `musl` toolchain
* Container runtime such as [Docker](https://www.docker.com/gettingstarted/), or [Rancher Desktop](https://docs.rancherdesktop.io/getting-started/installation/) installed and running.
* [Oracle GraalVM 25](https://www.graalvm.org/downloads/). We recommend using [SDKMAN!](https://sdkman.io/). (For other download options, see [GraalVM Downloads](https://www.graalvm.org/downloads/).)
    ```bash
    sdk install java 25-graal
    ```

## Setup

1. Clone this repository with Git and enter the application directory:
    ```bash
    git clone https://github.com/graalvm/workshops.git
    ```
    ```bash
    cd workshops/native-image/spring-boot-webserver
    ```

2. Unzip the static resources required for the application:
    ```bash
    unzip src/main/resources/static.zip -d src/main/resources
    ```

3. Run the _build-jar-eclipse-temurin.sh_ script from the application directory:
    ```bash
    ./build-jar-eclipse-temurin.sh
    ```
    Once the script finishes, a container image _eclipse-temurin-jar_ should be available.
    Check its size. It should be approximately **480MB**.
    ```bash
    docker images
    ```
    This container uses `eclipse-temurin:25` as a base image for the runtime, which contains a full JDK.
    Now that you have the demo application and the initial container size, proceed to the steps.

## **STEP 1**: Compile and Run the Application from a JAR File Inside a Container

Start by compiling and running the application from a JAR file inside a Docker container.
It requires a container image with a full JDK and runtime libraries.

### Explanation

The Dockerfile provided for this step pulls [container-registry.oracle.com/graalvm/jdk:25](https://docs.oracle.com/en/graalvm/jdk/25/docs/getting-started/container-images/) for the builder, and then `gcr.io/distroless/java25-debian13` for the runtime.
The entrypoint for this image is equivalent to `java -jar`, so only a path to a JAR file is specified in `CMD`.

### Action

1. Run the _build-jar-java-base.sh_ script from the application directory:
    ```bash
    ./build-jar-java-base.sh
    ```

2. Once the script finishes, a container image _distroless-java-base.jar_ should be available. Start the application using `docker run`:
    ```bash
    docker run --rm -p8080:8080 webserver:distroless-java-base.jar
    ```
    The container started in hundreds of milliseconds, **1.427 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C. (The Docker runs in attached mode.)

5. Check the container size:
    ```bash
    docker images
    ```
    ```
    REPOSITORY   TAG                        IMAGE ID       CREATED       SIZE
    webserver    distroless-java-base.jar   3794608e7fd5   2 hours ago   225MB
    ```
    Note that the website pages added **44MB** to the total size.

## **STEP 2**: Build and Run a Jlink Custom Runtime Image Inside a Container

In this step, you will create a custom runtime of this Spring Boot web server with Jlink and run it inside a container image.
See how much reduction in size you can gain.

### Explanation

`jlink` is a tool that generates a custom Java runtime image that contains only the platform modules that are required for your application.
This is one of the approaches to make applications more space efficient and cloud-friendly, introduced in Java 11.

The script _build-jlink.sh_ that runs `docker build` using the _Dockerfile.distroless-java-base.jlink_.
The Dockerfile runs two stages: first it generates a jlink custom runtime on a full JDK (`container-registry.oracle.com/graalvm/jdk:25`); then copies the runtime image folder along with static website pages into a distroless Java base image, and sets the entrypoint.
Distroless Java base image provides `glibc` and other libraries needed by the JDK, but not a full-blown JDK.

The application does not have to be modular, but you need to figure out which modules the application depends on to be able to `jlink` it.
In the builder stage, running on a full JDK, after compiling the project, Docker creates a file _cp.txt_ containing the classpath with all the dependencies:
```
RUN ./mvnw dependency:build-classpath -Dmdep.outputFile=cp.txt
```
Then, Docker runs the `jdeps` command with the classpath to check required modules for this Spring Boot application:
```bash
RUN jdeps --ignore-missing-deps -q  --recursive --multi-release 25 --print-module-deps --class-path $(cat cp.txt) target/webserver-0.0.1-SNAPSHOT.jar
```
Finally, Docker runs `jlink` to create a custom runtime in the specified output directory _jlink-jre_.
The `ENTRYPOINT` for the application would be `java` from the custom runtime.

### Action

1. Run the script:
    ```
    ./build-jlink.sh
    ```

2. Once the script finishes, a container image _distroless-java-base.jlink_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:distroless-java-base.jlink
    ```

    The container started in **1.352 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Compare the file size of container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                          IMAGE ID       CREATED       SIZE
    webserver    distroless-java-base.jlink   72ceaa8a8dfd   2 hours ago   178MB
    webserver    distroless-java-base.jar     3794608e7fd5   2 hours ago   225MB
    ```
    Jlink shrinked the `distroless-java-base.jar` container by **47MB**.

## **STEP 3**: Build and Run a Native Image Inside a Container Using Paketo Buildpacks

In this step, you will compile this Spring Boot application ahead of time with GraalVM Native Image and run it using Paketo Buildpacks container images.

### Explanation

Spring Boot supports building a native image in a container using the [Paketo Buildpack for Oracle](https://github.com/paketo-buildpacks/oracle) which provides GraalVM Native Image.

The mechanism is that the Paketo builder pulls the [Jammy Tiny Stack image](https://github.com/paketo-buildpacks/builder-jammy-tiny) (Ubuntu distroless-like image) which contains no buildpacks.
Then you point the **builder** image to the **creator** image.
For this workshop, you point to the [Paketo Buildpack for Oracle](https://github.com/paketo-buildpacks/oracle) explicitly requesting the Native Image tool.

If you open the _pom.xml_ file, you see the `spring-boot-maven-plugin` declaration added for you:
```xml
<configuration>
    <image>
    <builder>paketobuildpacks/builder-jammy-buildpackless-tiny</builder>
    <buildpacks>
        <buildpack>paketobuildpacks/oracle</buildpack>
        <buildpack>paketobuildpacks/java-native-image</buildpack>
    </buildpacks>
    </image>
</configuration>
```
When `java-native-image` is requested, the buildpack downloads Oracle GraalVM, which includes Native Image.
The [Paketo documentation provides several examples](https://paketo.io/docs/howto/java/#build-an-app-as-a-graalvm-native-image-application) that show you how to build applications with Native Image using buildpacks.

> Note that if you do not specify Oracle's buildpack, it will pull the default buildpack, which can result in reduced performance.

### Action

1. Build a native executable for this Spring application using the Paketo buildpack:
    ```bash
    ./mvnw -Pnative spring-boot:build-image
    ```

2. Once the build completes, a container image _webserver.buildpacks:latest_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 docker.io/library/webserver.buildpacks:latest
    ```

    The application is running from the native image inside a container. The container started in just **0.032 seconds**!

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY            TAG                          IMAGE ID       CREATED         SIZE
    webserver             distroless-java-base.jlink   191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar     846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                       615deed5b89c   45 years ago    142MB

    ```
    The new container, tagged as _webserver.buildpacks:latest_, is **142MB** smaller than the Jlink and distroless Java base versions.

## **STEP 4**: Build a Native Image and Run Inside a Container (Default Configuration)

In this step, you will create a native image with the default configuration and execute it inside a container.

### Explanation

Spring Boot 3 has integrated support for GraalVM Native Image, making it easier to set up and configure your project.
[Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html) project, maintained by the GraalVM team, provide Maven and Gradle plugins for building native images.
The project configuration already contains all necessary plugins, including [Native Image Maven plugin](https://graalvm.github.io/native-build-tools/latest/index.html):
```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

You can build this web server ahead of time into a native executable, on your host machine, just like this: `./mvnw -Pnative native:compile`.
The command will compile the application and create a fully dynamically linked native image, `webserver`, in the _target_ directory.

However, there is a Dockerfile, _Dockerfile.distroless-java-base.dynamic_, that runs the native image step inside the builder container, and then copies this native executable in a distroless base container with just enough to run the application. No Java Runtime Environment (JRE) is required!

**Distroless container images** contain only your application and its runtime dependencies.
They do not contain package managers, shells or any other programs you would expect to find in a standard Linux distribution.
Learn more in ["Distroless" Container Images](https://github.com/GoogleContainerTools/distroless).

### Action

1. Run the script:
    ```
    ./build-dynamic-image.sh
    ```

2. Once the build completes, a container image _distroless-java-base.dynamic_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:distroless-java-base.dynamic
    ```

    The application is running from the native image inside a container. The container started in  **0.031 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                            IMAGE ID       CREATED         SIZE
    webserver             distroless-java-base.dynamic   d7c449b9373d   2 hours ago     141MB
    webserver             distroless-java-base.jlink     191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar       846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                         615deed5b89c   45 years ago    142MB
    ```
    The new container image size, **141MB**, almost matches the size of the container built with Paketo Buildpacks.
    It is expected because it is the same native executable packaged into a different base container.

    The size of the native executable itself is **103MB**.
    Note that the static resources are "baked" into this native executable and added 44MB to its size.

## **STEP 5**: Build a Size-Optimized Native Image and Run Inside a Container

_This is where the fun begins._

In this step, you will build a fully dynamically linked native image **with the file size optimization on** and run it inside a container.

### Explanation

GraalVM Native Image provides the option `-Os` which optimizes the resulting native image for file size.
`-Os` enables `-O2` optimizations except those that can increase code or executable size significantly.
Learn more in [the Native Image documentation](https://www.graalvm.org/jdk25/reference-manual/native-image/optimizations-and-performance/#optimization-levels).

For that, a separate Maven profile is provided to differentiate this run from the default build, and giving a different name for the output file:
```xml
<profile>
    <id>dynamic-size-optimized</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <imageName>webserver.dynamic-optimized</imageName>
                    <buildArgs>
                        <buildArg>-Os</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

The Dockerfile for this step, _Dockerfile.distroless-java-base.dynamic-optimized_, creates a native image which is fully dynamically linked and **optimized for size** inside the builder container, and then packages it in a distroless base container with just enough to run the application. No Java Runtime Environment (JRE) is required.

> The `-Os` optimization will be on for all the subsequent builds.

### Action

1. Run the script to build a size-optimized native executable and package it into a container:
    ```bash
    ./build-dynamic-image-optimized.sh
    ```

2. Once the build completes, a container image _distroless-java-base.dynamic-optimized_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:distroless-java-base.dynamic-optimized
    ```

    The application is running from the native image inside a container. The container started in **0.051 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   2 hours ago     105MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   2 hours ago     141MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    142MB
    ```
    The size of `distroless-java-base.dynamic-optimized` container is cut down from **141MB** to **105MB**.
    This is because the native executable reduced in size.

    The size of the native executable decreased from **103MB** to **69MB** by applying the file size optimization!

## **STEP 6**: (Optional) Build a Size-Optimized Native Image with SkipFlow and Run Inside a Container

In this step, you will build another fully dynamically linked native image but with the **SkipFlow** and **file size** optimizations on. Then you run it inside a container.

### Explanation

As of Oracle GraalVM 25, more performance improvements are enabled by default. One of which is [SkipFlow](https://www.graalvm.org/release-notes/JDK_25/#native-image)-an extension to the Native Image static analysis that tracks primitive values and evaluates branching conditions dynamically during the process.

Note: The feature is enabled by default. With the previous releases, it could be controlled using these host options: `-H:+TrackPrimitiveValues` and `-H:+UsePredicates`.

For that, a separate Maven profile is provided, giving a different name for the output file:
```xml
<profile>
    <id>dynamic-skipflow-optimized</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <imageName>webserver.dynamic-skipflow</imageName>
                    <buildArgs>
                        <buildArg>-Os</buildArg>
                        <buildArg>-H:+UnlockExperimentalVMOptions</buildArg>
                        <buildArg>-H:+TrackPrimitiveValues</buildArg>
                        <buildArg>-H:+UsePredicates</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

The Dockerfile for this step, _Dockerfile.distroless-java-base.dynamic-skipflow_, is pretty much the same as before: running a native image build inside the builder container, and then copying it over to a distroless base container with just enough to run the application. No Java Runtime Environment (JRE) is required.

### Action

1. Run the script to build a size-optimized native executable and package it into a container:
    ```bash
    ./build-dynamic-image-skipflow.sh
    ```

2. Once the build completes, a container image _distroless-java-base.dynamic-optimized_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:distroless-java-base.dynamic-skipflow
    ```

    The application is running from the native image inside a container.
    The startup time has not changed.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             distroless-java-base.dynamic-skipflow    7c748db34ef4   2 hours ago     103MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   2 hours ago     105MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   2 hours ago     141MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    142MB
    ```
    The gain is tiny: the container size reduced only by 2MB, from 105MB to **103MB**, but depending on the application, **SkipFlow can provide up to a 4% reduction in binary size without any additional impact on build time**.

## **STEP 7**: Build a Size-Optimized Mostly Static Native Image and Run Inside a Container

In this step, you will build a **mostly static** native image, with the file size optimization on, and then package it into a container image that provides `glibc`, and run.

### Explanation

A **mostly static** native image links all the shared libraries on which it relies (`zlib`, JDK-shared static libraries) except the standard C library, `libc`.
This type of native image is useful for deployment on a distroless base container image.
You can build a mostly statically linked image by passing the `--static-nolibc` option at build time.

A separate Maven profile exists for this step:
```xml
<profile>
    <id>mostly-static</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <imageName>webserver.mostly-static</imageName>
                    <buildArgs>
                        <buildArg>--static-nolibc</buildArg>
                        <buildArg>-Os</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Action

1. Run the script:
    ```bash
    ./build-mostly-static-image.sh
    ```

2. Once the build completes, a container image _distroless-base.mostly-static_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:distroless-base.mostly-static
    ```

    The application is running from the mostly static native image inside a container. The container started in **0.052 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             distroless-base.mostly-static            2096b9d21750   2 hours ago     92.7MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   2 hours ago     105MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   2 hours ago     141MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    142MB
    ```

    The size of the new _distroless-base.mostly-static_ container is **92.7MB**.
    The reduction in size is related to the fact that a smaller base image was pulled: **gcr.io/distroless/base-debian13**.
    [Distroless images](https://github.com/GoogleContainerTools/distroless) are very small, and the one used is only **48.3 MB**.
    That's about 50% of the size of **java-base-debian13**(124 MB) used before, and 3 times less than **java25-debian13** (192 MB) containing a full JDK.

    The size of the mostly static native image has not changed, and is **69MB**.

## **STEP 7**: Build a Size-Optimized Fully Static Native Image and Run Inside a Container

In this step, you will build a **fully static** native image, with the file size optimization on, and then package it into a _scratch_ container.

### Explanation

A **fully static** native image is a statically linked binary that you can use without any additional library dependencies.
You can create a static native image by statically linking it against `musl-libc`, a lightweight, fast, and simple `libc` implementation.
To build a fully static executable, pass the `--static --libc=musl` options at build time.

A fully static image **does not rely on any libraries in the operating system environment** and can be packaged in the tiniest container.

It is easy to deploy on a slim or distroless container, even a [_scratch_ container](https://hub.docker.com/_/scratch).
A _scratch_ container is a [Docker official image](https://hub.docker.com/_/scratch), only 2MB in size, useful for building super minimal images.

A separate Maven profile exists for this step:
```xml
<profile>
    <id>static</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <imageName>webserver.static</imageName>
                    <buildArgs>
                        <buildArg>--static --libc=musl</buildArg>
                        <buildArg>-Os</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Action

1. Run the script to build a fully static native executable and package it into a _scratch_ container:
    ```bash
    ./build-static-image.sh
    ```

2. Once the build completes, a container image _scratch.static_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:scratch.static
    ```
    The starup time is the same as before, **0.055 seconds**, and, as a result, you get a tiny container with a fully functional and deployable server application!

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Now check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             scratch.static                           6cac69852631   2 hours ago     72.2MB
    webserver             distroless-base.mostly-static            2096b9d21750   2 hours ago     92.7MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   2 hours ago     105MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   2 hours ago     141MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    142MB
    ```

    The container size shrinked to **72.2MB**! A _scratch_ base image weights only **2MB**.

#### For Local Building

If you build a native image locally, it requires the `musl` toolchain with `zlib` installed on your machine.
We provide a script to download and configure the `musl` toolchain:
```bash
./setup-musl.sh
```

If you build a static native image locally, you can verify that is indeed fully static with `ldd`:
```bash
ldd target/webserver.static
```
You should see "not a dynamic executable" for the response.

## **STEP 8**: Compress a Static Native Image with UPX and Run Inside a Container

_Not convincing? What can you do next to reduce the size even more?_

In this step, you compress your fully static native image with UPX, then package into the same _scratch_ container, and run.

### Explanation

[UPX](https://upx.github.io/) - an advanced executable file compressor.
It can significantly reduce the executable size, but note, that UPX loads the executable into the memory, unpackages it, and then recompresses.

For local building, we provide a script to download and install UPX:
```bash
./setup-upx.sh
```

The Dockerfile for this step, _Dockerfile.scratch.static-upx_, builds a fully static native image inside the builder container and installs UPX in it.
Then Docker runs UPX which unpackages and recompresses this native executable.
Finally, the compressed executable copied over to the _scratch_ container, and executed.

### Action

1. Run the script to build a fully static native executable, compress it, and package it into a _scratch_ container:
   ```bash
    ./build-static-upx-image.sh
    ```

2. Once the build completes, a container image _scratch.static-upx_ should be available. Run it, mapping the ports:

    ```bash
    docker run --rm -p8080:8080 webserver:scratch.static-upx
    ```
    The container started in **0.048 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served as before!

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Lastly, check the size of all container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             scratch.static-upx                       becd77f9ea1a   2 hours ago     19.1MB
    webserver             scratch.static                           6cac69852631   2 hours ago     72.2MB
    webserver             distroless-base.mostly-static            2096b9d21750   2 hours ago     92.7MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   2 hours ago     105MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   2 hours ago     141MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     178MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     225MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    142MB
    ```
    The container size reduced dramatically to just **19.1MB**!
    The `upx` tool compressed the static native image by almost **50MB**, from 69MB to 19MB!
    The application and container image's size were now shrinked to the minimum.

## **STEP 9**: Clean up (Optional)

To clean up all images, run the `./clean.sh` script provided for that purpose.

## Conclusions

A fully functional and, at the same time, minimal, webserver application was compiled into a native Linux executable and packaged into base, distroless, and scratch containers thanks to GraalVM Native Image's support for various linking options.
All the versions of this Spring Boot application are functionally equivalent.

Sorted by size, it is clear that the fully static native image, compressed with `upx`, and then packaged on the _scratch_ container is the smallest at just **19.1MB**. Note that the website static pages added 44M to the container images size. Static resources are "baked” into the native image.

| Container                              | Size of a build artefact <br> (JAR, Jlink runtime, native executable) | Base image | Container |
|----------------------------------------|-----------------------------------------------------------------------|------------|-----------|
| eclispe-temurin-jar                    | webserver-0.0.1-SNAPSHOT.jar **32M**               | eclipse-temurin:25 201MB      | 481MB     |
| distroless-java-base.jar               | webserver-0.0.1-SNAPSHOT.jar **32M**               | java25-debian13 192MB         | 225MB     |
| distroless-java-base.jlink             | jlink-jre custom runtime **68MB**                  | java-base-debian13 128MB      | 178MB     |
| distroless-java-base.dynamic           | webserver.dynamic **103MB**                        | java-base-debian13 128MB      | 141MB     |
| webserver.buildpacks:latest            |                                                    |                               | 142MB     |
| distroless-java-base.dynamic-optimized | webserver.dynamic-optimized **69MB**               | java-base-debian13 128MB      | 105MB     |
| distroless-java-base.dynamic-skipflow  | webserver.dynamic-skipflow **67MB**                | java-base-debian13 128MB      | 103MB     |
| distroless-base.mostly-static          | webserver.mostly-static **69MB**                   | base-debian13 48.3MB          | 92.7MB    |
| scratch.static-alpine                  | webserver.static **69MB**                          | alpine:3 5MB                  | 80.5MB    |
| scratch.static                         | webserver.static **69MB**                          | scratch 2MB                   | 72.2MB    |
| scratch.static-upx                     | webserver.scratch.static-upx **19MB**              | scratch 2MB                   | 19.1MB    |

## Learn More

- [Static and Mostly Static Images](https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/)
- [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html)
- ["Distroless" Container Images](https://github.com/GoogleContainerTools/distroless)
- [Tiny Java Containers](https://github.com/graalvm/graalvm-demos/tree/master/native-image/tiny-java-containers)