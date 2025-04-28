# Multi-Cloud with GraalVM: Building and Deploying Optimized Applications

This workshop focuses on how to **build size-optimized native applications** using [GraalVM Native Image](https://www.graalvm.org/jdk24/reference-manual/native-image/) and how to leverage containerization to optimize the runtime environment.
You are going to discover ways to minimize application footprint by taking advantage of different Native Image linking options, and packaging a size-compact application into various containers, focusing on two main strategies: Distroless and static, discussing the trade-offs.
Every step is a multistage build using the [Oracle GraalVM container image](https://container-registry.oracle.com/ords/ocr/ba/graalvm) for the builder and different containers for the runner.

For the demo part, you will run a Spring Boot web server application, hosting the GraalVM website latest release documentation.
Spring Boot 3 has integrated support for GraalVM Native Image, making it easier to set up and configure a project.
Compiling a Spring Boot application ahead of time can significantly improve the performance and reduce its footprint.

### Workshop Objectives

In this workshop you will:

- Learn how to compile a Spring Boot application ahead of time into a native image and optimize it for file size.
- Use the latest [SkipFlow feature](https://www.graalvm.org/release-notes/JDK_24/#native-image) to optimize the file size even more, without any additional impact on build time. (As of GraalVM for JDK 24.)
- See how to use the [Maven plugin for Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).
- Create native executables and run them inside different Docker containers.
- Shrink a container image size by taking advantage of different Native Image linking options.
- Compare the deployed container images sizes.
- See how to use GitHub Actions to automate the build of native executables as part of a CI/CD pipeline.

### Prerequisites

* x86 Linux
* `musl` toolchain
* Container runtime such as [Docker](https://www.docker.com/gettingstarted/), or [Rancher Desktop](https://docs.rancherdesktop.io/getting-started/installation/) installed and running.
* [GraalVM for JDK 24](https://www.graalvm.org/downloads/). We recommend using [SDKMAN!](https://sdkman.io/). (For other download options, see [GraalVM Downloads](https://www.graalvm.org/downloads/).)
    ```bash
    sdk install java 24-graal
    ```

## Setup

Clone this repository with Git and enter the application directory:
```bash
git clone https://github.com/olyagpl/spring-boot-webserver.git
```
```bash
cd spring-boot-webserver
```

## **STEP 1**: Compile and Run the Application from a JAR File Inside a Container

Start by compiling and running the application from a JAR file inside a Docker container.
It requires a container image with a full JDK and runtime libraries.

### Explanation

The Dockerfile provided for this step pull [container-registry.oracle.com/graalvm/jdk:24](https://docs.oracle.com/en/graalvm/jdk/24/docs/getting-started/container-images/) for the builder, and then `gcr.io/distroless/java21-debian12` for the runtime.
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
    The container started in hundreds of milliseconds, **1.081 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C. (The Docker runs in attached mode.)

5. Check the container size:
    ```bash
    docker images
    ```
    ```
    REPOSITORY   TAG                        IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.jar   3794608e7fd5   21 minutes ago   235MB
    ```
    Note that the website pages added **44MB** to the total size.

## **STEP 2**: Build and Run a Jlink Custom Runtime Image Inside a Container

In this step, you will create a custom runtime of this Spring Boot web server with Jlink and run it inside a container image.
See how much reduction in size you can gain.

### Explanation

Jlink, or `jlink`, is a tool that generates a custom Java runtime image that contains only the platform modules that are required for your application.
This is one of the approaches to make applications more space efficient and cloud-friendly, introduced in Java 11.

The script _build-jlink.sh_ that runs `docker build` using the _Dockerfile.distroless-java-base.jlink_.
The Dockerfile runs two stages: first it generates a Jlink custom runtime on a full JDK (`container-registry.oracle.com/graalvm/jdk:24`); then copies the runtime image folder along with static website pages into a Distroless Java base image, and sets the entrypoint.
Distroless Java base image provides `glibc` and other libraries needed by the JDK, but not a full-blown JDK.

The application does not have to be modular, but you need to figure out which modules the application depends on to be able to `jlink` it.
In the builder stage, running on a full JDK, after compiling the project, Docker creates a file _cp.txt_ containing the classpath with all the dependencies:
```
RUN ./mvnw dependency:build-classpath -Dmdep.outputFile=cp.txt
```
Then, Docker runs the `jdeps` command with the classpath to check required modules for this Spring Boot application:
```bash
RUN jdeps --ignore-missing-deps -q  --recursive --multi-release 24 --print-module-deps --class-path $(cat cp.txt) target/webserver-0.0.1-SNAPSHOT.jar
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

    The container started in **1.102 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Compare the file size of container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                          IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.jlink   72ceaa8a8dfd   9 minutes ago    188MB
    webserver    distroless-java-base.jar     3794608e7fd5   47 minutes ago   235MB
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

2. Once the build completes, a container image _0.0.1-SNAPSHOT_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 docker.io/library/webserver.buildpacks:latest
    ```

    The application is running from the native image inside a container. The container started in just **0.031 seconds**!

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY            TAG                          IMAGE ID       CREATED            SIZE
    webserver             distroless-java-base.jlink   72ceaa8a8dfd   30 minutes ago     188MB
    webserver             distroless-java-base.jar     3794608e7fd5   About an hour ago  235MB
    webserver.buildpacks  latest                       10f1045f485b   45 years ago       161MB
    ```
    The new container, tagged as _webserver.buildpacks:latest_, is **158MB**, smaller than the Jlink and distroless Java base versions.

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
The command will compile the application and create a fully dynamically linked native image, `webserver`, in the _target/_ directory.

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

    The application is running from the native image inside a container. The container started in  **0.032 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                            IMAGE ID       CREATED         SIZE
    webserver             distroless-java-base.dynamic   d7c449b9373d   45 seconds ago  160MB
    webserver             distroless-java-base.jlink     191efb04958d   2 hours ago     188MB
    webserver             distroless-java-base.jar       846971900174   2 hours ago     235MB
    webserver.buildpacks  latest                         615deed5b89c   45 years ago    161MB
    ```
    The new container image size, **160MB**, almost matches the size of the container built with Paketo Buildpacks.
    It is expected because it is the same native executable packaged into a different base container.

    The size of the native executable itself is **122M**.
    Note that the static resources are "baked" into this native executable and added 44M to its size.

## **STEP 5**: Build a Size-Optimized Native Image and Run Inside a Container

_This is where the fun begins._

In this step, you will build a fully dynamically linked native image **with the file size optimization on** and run it inside a container.

### Explanation

GraalVM Native Image provides the option `-Os` which optimizes the resulting native image for file size.
`-Os` enables `-O2` optimizations except those that can increase code or executable size significantly.
Learn more in [the Native Image documentation](https://www.graalvm.org/jdk24/reference-manual/native-image/optimizations-and-performance/#optimization-levels).

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

    The application is running from the native image inside a container. The container started in **0.035 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   23 seconds ago  125MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   45 seconds ago  160MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     188MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     235MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    161MB
    ```
    The size of `distroless-java-base.dynamic-optimized` container is cut down from **164MB** to **130MB**.
    This is because the native executable reduced in size.

    The size of the native executable decreased from **125M** to **92M**  by applying the file size optimization!

## **STEP 6**: Build a Size-Optimized Native Image with SkipFlow and Run Inside a Container

In this step, you will build another fully dynamically linked native image but with the **SkipFlow** and **file size** optimizations on. Then you run it inside a container.

### Explanation

As of GraalVM for JDK 24, you can enable [SkipFlow](https://www.graalvm.org/release-notes/JDK_24/#native-image)-an extension to the Native Image static analysis that tracks primitive values and evaluates branching conditions dynamically during the process.

The feature is experimental and can be enabled with the following host options: `-H:+TrackPrimitiveValues` and `-H:+UsePredicates`.

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
    webserver             distroless-java-base.dynamic-skipflow    7c748db34ef4   3 minutes ago   123MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   23 seconds ago  125MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   45 seconds ago  160MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     188MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     235MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    161MB
    ```
    The gain is tiny: the container size reduced only by 2MB, from 125MB to **123MB**, but depending on the application, **SkipFlow can provide up to a 4% reduction in binary size without any additional impact on build time**.

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

    The application is running from the mostly static native image inside a container. The container started in **0.036 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             distroless-base.mostly-static            2096b9d21750   6 seconds ago   112MB
    webserver             distroless-java-base.dynamic-skipflow    7c748db34ef4   3 minutes ago   123MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   23 seconds ago  125MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   45 seconds ago  160MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     188MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     235MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    161MB
    ```

    The size of the new _distroless-base.mostly-static_ container is **112MB**.
    The reduction in size is related to the fact that a smaller base image was pulled: **gcr.io/distroless/base-debian12**.
    [Distroless images](https://github.com/GoogleContainerTools/distroless) are very small, and the one used is only **48.3 MB**.
    That's about 50% of the size of **java-base-debian12**(124 MB) used before, and 3 times less than **java21-debian12** (192 MB) containing a full JDK.

    The size of the mostly static native image has not changed, and is **92MB**.

## **STEP 8**: Build a Size-Optimized Fully Static Native Image and Run Inside a Container

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
    <id>fully-static</id>
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
    The starup time is the same as before, **0.035 seconds**, and, as a result, you get a tiny container with a fully functional and deployable server application!

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Now check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             scratch.static                           6cac69852631   3 minutes ago   91.8MB
    webserver             distroless-base.mostly-static            2096b9d21750   6 seconds ago   112MB
    webserver             distroless-java-base.dynamic-skipflow    7c748db34ef4   3 minutes ago   123MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   23 seconds ago  125MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   45 seconds ago  160MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     188MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     235MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    161MB
    ```

    The container size shrinked to **91.8MB**! A _scratch_ base image weights only **2MB**.

#### For Local Building

If you build a native image locally, it requires the `musl` toolchain with `zlib` installed on your machine.
We provide a script to download and configure the `musl` toolchain, and install `zlib` into the toolchain:
```bash
./setup-musl.sh
```

If you build a static native image locally, you can verify that is indeed fully static with `ldd`:
```bash
ldd target/webserver.static
```
You should see "not a dynamic executable" for the response.

## **STEP 9**: Compress a Static Native Image with UPX and Run Inside a Container

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
    The container started in **0.035 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served as before!

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Lastly, check the size of all container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY            TAG                                      IMAGE ID       CREATED         SIZE
    webserver             scratch.static-upx                       becd77f9ea1a   1 second ago    34.5MB
    webserver             scratch.static                           6cac69852631   3 minutes ago   91.8MB
    webserver             distroless-base.mostly-static            2096b9d21750   6 seconds ago   112MB
    webserver             distroless-java-base.dynamic-skipflow    7c748db34ef4   3 minutes ago   123MB
    webserver             distroless-java-base.dynamic-optimized   5e16a58b1649   23 seconds ago  125MB
    webserver             distroless-java-base.dynamic             d7c449b9373d   45 seconds ago  160MB
    webserver             distroless-java-base.jlink               191efb04958d   2 hours ago     188MB
    webserver             distroless-java-base.jar                 846971900174   2 hours ago     235MB
    webserver.buildpacks  latest                                   615deed5b89c   45 years ago    161MB
    ```
    The container size reduced dramatically to just **34.5MB**!
    The `upx` tool compressed the static native image by almost **57MB**.
    The application and container image's size were now shrinked to the minimum.

## **STEP 10**: Clean up (Optional)

To clean up all images, run the `./clean.sh` script provided for that purpose.

## Conclusions

A fully functional and, at the same time, minimal, webserver application was compiled into a native Linux executable and packaged into base, distroless, and scratch containers thanks to GraalVM Native Image's support for various linking options.
All the versions of this Spring Boot application are functionally equivalent.

Sorted by size, it is clear that the fully static native image, compressed with `upx`, and then packaged on the _scratch_ container is the smallest at just **34.5MB**. Note that the website static pages added 44M to the container images size. Static resources are "baked” into the native image.

| Container                              | Size of a build artefact <br> (JAR, Jlink runtime, native executable) | Base image | Container |
|----------------------------------------|-----------------------------------------------------------------------|------------|-----------|
| eclispe-temurin-jar                    | webserver-0.0.1-SNAPSHOT.jar **42MB**              | eclipse-temurin:21 201 MB     | 522MB     |
| distroless-java-base.jar               | webserver-0.0.1-SNAPSHOT.jar **42MB**              | java21-debian12 192MB         | 235MB     |
| distroless-java-base.jlink             | jlink-jre custom runtime **68MB**                  | java-base-debian12 128MB      | 188MB     |
| distroless-java-base.dynamic           | webserver.dynamic **122MB**                        | java-base-debian12 128MB      | 161MB     |
| webserver.buildpacks:latest            |                                                    |                               | 161MB     |
| distroless-java-base.dynamic-optimized | webserver.dynamic-optimized **88MB**               | java-base-debian12 128MB      | 125MB     |
| distroless-java-base.dynamic-skipflow  | webserver.dynamic-skipflow **86MB**                | java-base-debian12 128MB      | 123MB     |
| distroless-base.mostly-static          | webserver.mostly-static **88MB**                   | base-debian12 48.3MB          | 112MB     |
| scratch.static-alpine                  | webserver.static **88MB**                          | alpine:3 5MB                  | 100MB     |
| scratch.static                         | webserver.static **88MB**                          | scratch 2MB                   | 91.8MB    |
| scratch.static-upx                     | webserver.scratch.static-upx **33MB**              | scratch 2MB                   | 34.5MB    |

## Learn More

- [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html)
- ["Distroless" Container Images](https://github.com/GoogleContainerTools/distroless)
- [Paketo Buildpacks](https://paketo.io/docs/)
- [Static and Mostly Static Images](https://www.graalvm.org/jdk24/reference-manual/native-image/guides/build-static-executables/)
- [Tiny Java Containers by Shaun Smith at DevoxxUK 2022](https://youtu.be/6wYrAtngIVo)