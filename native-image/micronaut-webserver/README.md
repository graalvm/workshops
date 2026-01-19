# From JIT to Native: Efficient Java Containers with GraalVM and Micronaut

This workshop demonstrates how to **build efficient, size-optimized native applications** with [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) and the [Micronaut® framework](https://micronaut.io/), and deploy them in various containers to optimize the runtime environment.
Micronaut has first-class support for Native Image, simplifying project setup and configuration.

In this example, we serve static assets (GraalVM documentation pages) with Micronaut directly from the resources folder in the classpath.

Each step in this workshop is implemented as a multistage build.
It uses the [Oracle GraalVM container image](https://container-registry.oracle.com/ords/ocr/ba/graalvm) as the builder and explores different base images for the runner.
Each step is automated via scripts and Dockerfiles, and the application serves real, production-like content.

Yet, once compiled into a JAR and placed in a Docker container with a full JDK, this web server weighs approximately ~470MB.
From there, you will iteratively reduce its size by testing alternative packaging strategies: replacing the JVM with custom runtimes, using native executables, and ultimately producing fully static binaries.

### Objectives

In this workshop you will:

- Deploy the application in different containers to optimize the runtime environment.
- Compile a Micronaut application ahead of time into a native image and optimize it for file size.
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
    cd workshops/native-image/micronaut-webserver
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
    Check its size. It should be **472MB**.
    ```bash
    docker images
    ```
    This container uses `eclipse-temurin:25` as a base image for the runtime, which contains a full JDK.
    Now that you have the demo application and the initial container size, proceed to the steps.

## **STEP 1**: Compile and Run the Application from a JAR File Inside a Container

Start by compiling and running the application from a JAR file inside a Docker container.
It requires a container image with a JDK and runtime libraries.

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
    The container started in hundreds of milliseconds, **378ms**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

   ![GraalVM documentation pages](images/screens-demo.png)

4. Return to the terminal and stop the running container by clicking CTRL+C. (The Docker runs in attached mode.)

5. Check the container size:
    ```bash
    docker images
    ```
    ```
    REPOSITORY   TAG                        IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.jar   e285476a8266   32 minutes ago   216MB
    webserver    eclipse-temurin-jar        f6eef8d2aa40   33 minutes ago   472MB
    ```
    Note that the website pages added **44MB** to the overall image size.
    **216MB** is not bad for Java, but not great if you are optimizing for cold start and footprint.

## **STEP 2**: Build and Run a `jlink` Custom Runtime Image Inside a Container

In this step, you will create a custom runtime for this Micronaut web application with `jlink` and run it inside a container image.
See how much reduction in size you can gain.

### Explanation

`jlink` is a tool that generates a custom Java runtime image that contains only the platform modules that are required for your application.
Introduced in Java 11, it provides a way to make applications more space efficient and cloud-friendly.

The script _build-jlink.sh_ that runs `docker build` using the _Dockerfile.distroless-java-base.jlink_.
The Dockerfile contains two stages: first it generates a `jlink` custom runtime on a full JDK (`container-registry.oracle.com/graalvm/jdk:25`); then copies the runtime image folder along with static assets into a distroless Java base image, and sets the entrypoint.
Distroless Java base image provides `glibc` and other libraries needed by the JDK, **but not a full-blown JDK**.

The application does not have to be modular, but you need to figure out which modules the application depends on to be able to `jlink` it.
In the builder stage, running on a full JDK, after compiling the project, Docker creates a file _cp.txt_ containing the classpath with all the dependencies:
```
RUN ./mvnw dependency:build-classpath -Dmdep.outputFile=cp.txt
```
Then, runs `jlink` to create a custom runtime in the specified output directory _jlink-jre_, by using the output of the `jdeps` command to obtain the required modules for this Micronaut application:
```bash
RUN CP=$(cat cp.txt) && \
    MODULES=$(jdeps --ignore-missing-deps -q --recursive --multi-release 25 --print-module-deps --class-path "$CP" target/webserver-0.1.jar) && \
    echo "Modules: $MODULES" && \
    jlink \
      --module-path "${JAVA_HOME}/jmods" \
      --add-modules "$MODULES",jdk.zipfs \
      --verbose \
      --strip-debug \
      --compress zip-9 \
      --no-header-files \
      --no-man-pages \
      --strip-java-debug-attributes \
      --output jlink-jre
```
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

    The container started in **340ms**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Compare the file size of container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                          IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.jlink   2a33effa1e0f   3 minutes ago    167MB
    webserver    distroless-java-base.jar     e285476a8266   32 minutes ago   216MB
    webserver    eclipse-temurin-jar          f6eef8d2aa40   33 minutes ago   472MB
    ```
    `jlink` shrank the `distroless-java-base.jar` container by **49MB**.
    There is no dramatic performance change, but a solid step toward efficiency.

## **STEP 3**:  Build a Native Image and Run Inside a Container (Default Configuration)

In this step, you will compile the Micronaut application ahead of time using GraalVM Native Image, with the default configuration, and run it inside a container.

### Explanation

Micronaut provides built-in support for GraalVM Native Image.
For example, when generating a new project with [Micronaut Launch](https://micronaut.io/launch/), you add **graalvm** as a packaging feature.
To create a native executable for this application using Maven, on your machine, simply run:
```bash
./mvnw package -Dpackaging=native-image
```
This command compiles the application ahead of time and produces a fully dynamically linked native image in the _target/_ director.

For this step, we provide a Dockerfile, _Dockerfile.distroless-java-base.dynamic_, that runs the native image build step inside the builder container, and then copies this native executable in a distroless base container with just enough to run the application.
No Java Runtime Environment (JRE) is required!

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

The application is running from the native image inside a container. The container started in **20ms**!

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY   TAG                            IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.dynamic   53af84312571   5 minutes ago    132MB
    webserver    distroless-java-base.jlink     dde1eb772aa5   12 minutes ago   167MB
    webserver    distroless-java-base.jar       e285476a8266   32 minutes ago   216MB
    webserver    eclipse-temurin-jar            f6eef8d2aa40   33 minutes ago   472MB
    ```
    The new container image size is **132MB** and contains a dynamically linked native executable of this Micronaut web server.
    The executable file size is **86MB**.
    Note that the static resources are "baked" into this native executable.

    Ahead-of-time compilation not only reduced the container size by **35MB**, but also cut startup time by almost 20×—from hundreds of milliseconds to just 20.
    That's how powerful native compilation can be!

## **STEP 4**: Build a Size-Optimized Native Image and Run Inside a Container

_This is where the fun begins._

In this step, you will build a fully dynamically linked native image **with the file size optimization on** and run it inside a container.

### Explanation

GraalVM Native Image provides the option `-Os` which optimizes the resulting native image for file size.
`-Os` enables `-O2` optimizations except those that can increase code or executable size significantly.
Learn more about different optimization levels in the [Native Image documentation](https://www.graalvm.org/jdk25/reference-manual/native-image/optimizations-and-performance/#optimization-levels).

To configure the Native Image build and have more manual control over the process, GraalVM provides the [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html): Maven and Gradle plugins for building native images.

The project configuration already contains the [Native Image Maven plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) declaration, within a Maven profile.
[Maven profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html) are a great way to have different build configurations within a single _pom.xml_ file.

For convenience, you can have a separate Maven profile for each step, adding the plugin into it.
This way you can differentiate from the default build, and give a different name for the output file.

The snippet below shows the profile with the plugin declaration and build configuration for this step:
```xml
  <profile>
    <id>dynamic-optimized</id>
    <build>
      <plugins>
        <plugin>
          <groupId>org.graalvm.buildtools</groupId>
          <artifactId>native-maven-plugin</artifactId>
          <version>${native.maven.plugin.version}</version>
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

> An alternative way to pass build options to Native Image, without creating Maven profiles, is by using `-DbuildArgs`:
```bash
./mvnw package -Dpackaging=native-image -DbuildArgs="-Os,-o target/webserver.dynamic-optimized"
```

The Dockerfile for this step, _Dockerfile.distroless-java-base.dynamic-optimized_, creates a native image which is fully dynamically linked and **optimized for size** inside the builder container, and then packages it in a distroless base container with just enough to run the application.
No Java Runtime Environment (JRE) is required.

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

    The application is running from the native image inside a container. The container started in the same **20ms**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY   TAG                                      IMAGE ID       CREATED         SIZE
    webserver    distroless-java-base.dynamic-optimized   5e16a58b1649   10 minutes ago  102MB
    webserver    distroless-java-base.dynamic             d7c449b9373d   12 minutes ago  132MB
    webserver    distroless-java-base.jlink               dde1eb772aa5   15 minutes ago  167MB
    webserver    distroless-java-base.jar                 e285476a8266   32 minutes ago  216MB
    webserver    eclipse-temurin-jar                      f6eef8d2aa40   33 minutes ago  472MB
    ```

    The size of the container came down from **132MB** to **102MB**.
    The executable size decreased by **24MB** (from 86MB to 62MB) just by applying the file size optimization - with no change in behavior or startup time!


## **STEP 5**: (Optional) Build a Size-Optimized Native Image with SkipFlow and Run Inside a Container

In this step, you will build another fully dynamically linked native image but with the **SkipFlow** and **file size** optimizations on. Then you run it inside a container.

### Explanation

As of Oracle GraalVM 25, more performance improvements are enabled by default.
One of which is [SkipFlow](https://www.graalvm.org/release-notes/JDK_25/#native-image)-an extension to the Native Image static analysis that tracks primitive values and evaluates branching conditions dynamically during the process.

Note: The feature is enabled by default. With the previous releases, it could be controlled using these host options: `-H:+TrackPrimitiveValues` and `-H:+UsePredicates`.

For this, we have added a separate Maven profile with a different name for the generated native executable:
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

> An alternative way to pass build options to Native Image, without creating Maven profiles, is by using `-DbuildArgs`:
```bash
./mvnw package -Dpackaging=native-image -DbuildArgs="-Os,-H:+UnlockExperimentalVMOptions,-H:+TrackPrimitiveValues,-H:+UsePredicates,-o target/webserver.dynamic-skipflow"
```

The Dockerfile for this step, _Dockerfile.distroless-java-base.dynamic-skipflow_, is pretty much the same as before: running a native image build inside the builder container, and then copying it over to a distroless base container with just enough to run the application.
No Java Runtime Environment (JRE) is required.

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
    REPOSITORY   TAG                                      IMAGE ID       CREATED         SIZE
    webserver    distroless-java-base.dynamic-skipflow    6caada87f616   8  minutes ago  101MB
    webserver    distroless-java-base.dynamic-optimized   5e16a58b1649   10 minutes ago  102MB
    webserver    distroless-java-base.dynamic             d7c449b9373d   12 minutes ago  132MB
    webserver    distroless-java-base.jlink               dde1eb772aa5   15 minutes ago  167MB
    webserver    distroless-java-base.jar                 e285476a8266   32 minutes ago  216MB
    webserver    eclispe-temurin-jar                      f6eef8d2aa40   33 minutes ago  472MB
    ```
    The gain is tiny: the container size reduced only by 1MB, but depending on the application, **SkipFlow can provide up to a 4% reduction in binary size without any additional impact on build time**.

## **STEP 6**: Build a Size-Optimized Mostly Static Native Image and Run Inside a Container

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
                <version>${native.maven.plugin.version}</version>
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

> An alternative way to pass build options to Native Image, without creating Maven profiles, is by using `-DbuildArgs`:
```bash
./mvnw package -Dpackaging=native-image -DbuildArgs="--static-nolibc,-Os,-o target/webserver.mostly-static
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

    The application is running from the mostly static native image inside a container. The startup time has not changed, around **20ms**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY   TAG                                      IMAGE ID       CREATED         SIZE
    webserver    distroless-base.mostly-static            af0a790a6558   7 minutes ago   89.7MB
    webserver    distroless-java-base.dynamic-optimized   5e16a58b1649   10 minutes ago  102MB
    webserver    distroless-java-base.dynamic             d7c449b9373d   12 minutes ago  132MB
    webserver    distroless-java-base.jlink               dde1eb772aa5   15 minutes ago  167MB
    webserver    distroless-java-base.jar                 e285476a8266   32 minutes ago  216MB
    webserver    eclipse-temurin-jar                      f6eef8d2aa40   33 minutes ago  472MB
    ```

    The size of the new _distroless-base.mostly-static_ container is **89.7MB**.
    The reduction in size is related to the fact that a smaller base image was pulled: **gcr.io/distroless/base-debian12**.
    [Distroless images](https://github.com/GoogleContainerTools/distroless) are very small, and the one used is only **48.3 MB**.
    The size of the mostly static native image has not changed, and is **62MB**.

## **STEP 6**: Build a Size-Optimized Fully Static Native Image and Run Inside a Container

In this step, you will build a **fully static** native image, with the file size optimization on, and then package it into a _scratch_ container.

### Explanation

A **fully static** native image is a statically linked binary that you can use without any additional library dependencies.
You can create a static native image by statically linking it against `musl-libc`, a lightweight, fast, and simple `libc` implementation.
To build a fully static executable, pass the `--static --libc=musl` options at build time.

A fully static image **does not rely on any libraries in the operating system environment** and can be packaged in the tiniest container.

It is easy to deploy on a slim or distroless container, even a [_scratch_ container](https://hub.docker.com/_/scratch).
A _scratch_ container, an official Docker image, is only 2MB in size. It is basically an empty file system, useful for building super minimal images.

A separate Maven profile exists for this step:
```xml
<profile>
    <id>static</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>${native.maven.plugin.version}</version>
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

> An alternative way to pass build options to Native Image, without creating Maven profiles, is by using `-DbuildArgs`:
```bash
./mvnw package -Dpackaging=native-image -DbuildArgs="--static --libc=musl,-Os,-o target/webserver.static"
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
    The startup time is the same as before, and, as a result, you get a tiny container with a fully functional and deployable server application!

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Now check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY   TAG                                      IMAGE ID       CREATED         SIZE
    webserver    scratch.static                           ead2b9ac1bb8   6 minutes ago   69.2MB
    webserver    distroless-base.mostly-static            af0a790a6558   7 minutes ago   89.7MB
    webserver    distroless-java-base.dynamic-optimized   5e16a58b1649   10 minutes ago  102MB
    webserver    distroless-java-base.dynamic             d7c449b9373d   12 minutes ago  132MB
    webserver    distroless-java-base.jlink               dde1eb772aa5   15 minutes ago  167MB
    webserver    distroless-java-base.jar                 e285476a8266   32 minutes ago  216MB
    webserver    eclipse-temurin-jar                      f6eef8d2aa40   33 minutes ago  472MB
    ```
    A production-ready Micronaut web application was deployed in under **69.2MB**, starting in 20 milliseconds!

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

## **STEP 7**: Going Extreme: Compress a Static Native Image with UPX and Run Inside a Container

_Not convincing enough? What can you do next to reduce the size even more?_

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
    The container started in **20ms**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served as before!

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Lastly, check the size of all container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY   TAG                                      IMAGE ID       CREATED         SIZE
    webserver    scratch.static-upx                       31accca9ac49   11 seconds ago  22.3MB
    webserver    scratch.static                           ead2b9ac1bb8   6 minutes ago   69.2MB
    webserver    distroless-base.mostly-static            af0a790a6558   7 minutes ago   89.7MB
    webserver    distroless-java-base.dynamic-optimized   5e16a58b1649   10 minutes ago  102MB
    webserver    distroless-java-base.dynamic             d7c449b9373d   12 minutes ago  132MB
    webserver    distroless-java-base.jlink               dde1eb772aa5   15 minutes ago  167MB
    webserver    distroless-java-base.jar                 e285476a8266   32 minutes ago  216MB
    webserver    eclipse-temurin-jar                      f6eef8d2aa40   33 minutes ago  472MB
    ```
    The container size reduced dramatically to just **22.3MB**!
    The `upx` tool compressed the static native image from **62MB** to just **20MB**.
    That’s nearly 20× smaller than the original container size.
    The application still started instantly and served requests flawlessly!

## **STEP 8**: Clean up (Optional)

To clean up all images, run the `./clean.sh` script provided for that purpose.

## Conclusions

A fully functional and, at the same time, minimal, webserver application was compiled into a native Linux executable and packaged into base, distroless, and scratch containers thanks to GraalVM Native Image's support for various linking options.
All the versions of this Micronaut web application are functionally equivalent.

Sorted by size, it is clear that the fully static native image, compressed with `upx`, and then packaged on the _scratch_ container is the smallest at just **22.3MB**.
Note that the website static pages add 44MB to the container images size. Static resources are "baked” into the native image.

| Container                              | Size of a build artefact <br> (JAR, Jlink runtime, native executable) | Base image | Container |
|----------------------------------------|-----------------------------------------------------------------------|------------|-----------|
| eclispe-temurin-jar                    | webserver-0.1.jar **24MB**                         | eclipse-temurin:25 201MB      | 472MB     |
| distroless-java-base.jar               | webserver-0.1.jar **24MB**                         | java25-debian13 192MB         | 216MB     |
| distroless-java-base.jlink             | jlink-jre custom runtime **68MB**                  | java-base-debian13 128MB      | 167MB     |
| distroless-java-base.dynamic           | webserver.dynamic **86MB**                         | java-base-debian13 128MB      | 132MB     |
| distroless-java-base.dynamic-optimized | webserver.dynamic-optimized **62MB**               | java-base-debian13 128MB      | 102MB     |
| distroless-java-base.dynamic-skipflow  | webserver.dynamic-skipflow **61MB**                | java-base-debian13 128MB      | 101MB     |
| distroless-base.mostly-static          | webserver.mostly-static **62MB**                   | base-debian13 48.3MB          | 89.7MB    |
| scratch.static                         | webserver.static **62MB**                          | scratch 2MB                   | 69.2MB    |
| scratch.static-upx                     | webserver.scratch.static-upx **20MB**              | scratch 2MB                   | 22.3MB    |

## Learn More

- [Serving static resources in a Micronaut Framework application](https://guides.micronaut.io/latest/micronaut-static-resources-gradle-java.html)
- [Static and Mostly Static Images](https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/)
- [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html)
- [Tiny Java Containers](https://github.com/graalvm/graalvm-demos/tree/master/native-image/tiny-java-containers)
