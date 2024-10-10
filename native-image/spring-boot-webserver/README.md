# Multicloud Apps with GraalVM - Up and Running

This workshop is for developers looking to understand how to **build size-optimized cloud native Java applications** using [GraalVM Native Image](https://www.graalvm.org/jdk23/reference-manual/native-image/).

In this workshop, you will discover ways to minimize application footprint using different Native Image linking options and packaging into various base containers. You will run a Spring Boot web server application hosting the GraalVM documentation website. 

Spring Boot 3 has integrated support for GraalVM Native Image, making it easier to set up and configure a project.
Compiling a Spring Boot application ahead of time can significantly boost performance and reduce the footprint.

### Workshop Objectives

In this workshop you will:

- Learn how to compile a Spring Boot 3 application ahead of time into a native executable and optimize it for file size.
- See how to use the [GraalVM Native Image Maven Plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).
- Create native executables and run them inside different Docker containers.
- Shrink the container image size by using different Native Image containerisation and linking options.
- Compare the deployed container images sizes.
- See how to use GitHub Actions to automate the build of native executables as part of a CI/CD pipeline.

### Prerequisites

* x86 Linux
* `musl` toolchain
* Container runtime such as [Rancher Desktop](https://docs.rancherdesktop.io/getting-started/installation/) or [Docker](https://www.docker.com/gettingstarted/) installed and running
* [GraalVM for JDK 23](https://www.graalvm.org/downloads/) or later. We recommend using [SDKMAN!](https://sdkman.io/). (For other download options, see [GraalVM Downloads](https://www.graalvm.org/downloads/).)
    ```bash
    sdk install java 23-graal
    ```

## Setup

Clone this repository with Git and enter the application directory:

```bash
git clone https://github.com/graalvm/workshops.git
```

```bash
cd workshops/native-image/spring-boot-webserver
```

## **STEP 1**: Compile and Run the Application from a JAR File Inside a Container

Start by compiling and running the application from a JAR file inside a Docker container. 
It requires a container image with a full JDK and runtime libraries. 

### Explanation

The Dockerfile, provided for this step, _Dockerfile.distroless-base.uber-jar_, uses [Oracle GraalVM for JDK 23 container image](https://docs.oracle.com/en/graalvm/jdk/23/docs/getting-started/container-images/) for the builder, and [Debian Slim Linux image](https://github.com/linuxcontainers/debian-slim) for the runtime.
The entrypoint for this image is equivalent to `java -jar`, so only a path to a JAR file is specified in `CMD`.

### Action

1. Run the _build-jar.sh_ script from the application directory:
    ```bash
    ./build-jar.sh
    ```

2. Once the script finishes, a container image _debian-slim.jar_ should be available. Start the application using `docker run`:
    ```bash
    docker run --rm -p8080:8080 webserver:debian-slim.jar
    ```
    The container started in hundreds of milliseconds, **1.135 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C. (The Docker runs in an attached mode.)

5. Check the container size:
    ```bash
    docker images
    ```
    The expected size is ~240MB. 
    Note that the website pages added **44MB** to the container size. 
    ```
    REPOSITORY   TAG               IMAGE ID       CREATED             SIZE
    webserver    debian-slim.jar   5c69f06a3972   About an hour ago   238MB
    ```

## **STEP 2**: Build and Run a Jlink Custom Runtime Image Inside a Container
 
In this step, you will create a custom runtime of this Spring Boot web server with Jlink and run it inside a container image.
See how much reduction in size you can gain.

### Explanation

Jlink, or `jlink`, is a tool that generates a custom Java runtime image that contains only the platform modules that are required for your application. This is one of the approaches to create cloud native applications introduced in Java 11.

The script _build-jlink.sh_ runs `docker build` using the _Dockerfile.distroless-java-base.jlink_.
The Dockerfile contains a multistage build: generates a Jlink custom runtime on a full JDK, copies the runtime image folder along with static website pages into a Java base container image, and sets the entrypoint.

The application does not have to be modular, but you need to figure out which modules the application depends on to be able to `jlink` it. Shown below are three commands from the Dockerfile:
- In the build stage, running on a full JDK, after compiling the project, generates a file _cp.txt_ containing the classpath with all the dependencies:
    ```
    RUN ./mvnw dependency:build-classpath -Dmdep.outputFile=cp.txt
    ```
- Then, runs the `jdeps` command with the classpath to check required modules for this Spring Boot application:
    ```bash
    RUN jdeps --ignore-missing-deps -q  --recursive --multi-release 23 --print-module-deps --class-path $(cat cp.txt) target/webserver-0.0.1-SNAPSHOT.jar
    ```
- Finally, runs `jlink` to create a custom runtime in the specified output directory _jlink-jre_.
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

    The container started in **1.224 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Compare the file size of container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                          IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.jlink   687f7683ad58   16 minutes ago   192MB
    webserver    debian-slim.jar              5c69f06a3972   2 hours ago      238MB
    ```
    Jlink shrank the container by **46MB**.

## **STEP 3**: Build a Native Image Locally and Run Inside a Container (Default Configuration)

In this step, you will create a native image with the default configuration on a host machine, and only run it inside a container.

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

You can build this web server ahead of time into a native executable, on your host machine, just like this: `./mvnw -Pnative native:compile`. The command will compile the application and create a fully dynamically linked native image, `webserver`, in the _target/_ directory.

However, there is a script _build-dynamic-image.sh_, for your convenience, that does that and packages this native image in a distroless base container image with just enough to run the application. No Java Runtime Environment (JRE) is required! 

**Distroless container images** contain only your application and its runtime dependencies. They do not contain package managers, shells or any other programs you would expect to find in a standard Linux distribution.
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

    The application is running from the native image inside a container. The container started in  **0.033 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```bash
    REPOSITORY   TAG                            IMAGE ID       CREATED          SIZE
    webserver    distroless-java-base.dynamic   5a5de47579ef   2 minutes ago    164MB
    webserver    distroless-java-base.jlink     687f7683ad58   37 minutes ago   192MB
    webserver    debian-slim.jar                5c69f06a3972   2 hours ago      238MB
    webserver    0.0.1-SNAPSHOT                 0660806da4a2   44 years ago     163MB
    ```
    The new container image size **164MB** it almost matches the size of the container built with Paketo Buildpacks. 
    It is expected because it is the same native executable packaged into a different base container.

6. Check the size of the native executable:
    ```bash
    ls -lh target/webserver*
    ```
    The expected size is **125M**. 
    Note that the static resources are "baked" into this native executable and added 44M to its size.

## **STEP 4**: Build a Size-Optimized Native Image Locally and Run Inside a Container

_This is where the fun begins._

In this step, you will build a fully dynamically linked native image **with the file size optimization on**, on a host machine, and run it inside a container.

### Explanation

GraalVM Native Image provides the option `-Os` which optimizes the resulting native image for file size. 
`-Os` enables `-O2` optimizations except those that can increase code or executable size significantly. Learn more in [the Native Image documentation](https://www.graalvm.org/jdk23/reference-manual/native-image/optimizations-and-performance/#optimization-levels).

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
                    <imageName>webserver.dynamic</imageName>
                    <buildArgs>
                        <buildArg>-Os</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

The script _build-dynamic-image.sh_, available in this repository for your convenience, creates a native image with fully dynamically linked shared libraries, **optimized for size**, and then packages it in a distroless base container image with just enough to run the application. No Java Runtime Environment (JRE) is required.

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
    REPOSITORY   TAG                                      IMAGE ID       CREATED              SIZE
    webserver    distroless-java-base.dynamic-optimized   0bed48dadd11   About a minute ago   130MB
    webserver    distroless-java-base.dynamic             5a5de47579ef   16 minutes ago       164MB
    webserver    distroless-java-base.jlink               687f7683ad58   51 minutes ago       192MB
    webserver    debian-slim.jar                          5c69f06a3972   2 hours ago          238MB
    webserver    0.0.1-SNAPSHOT                           0660806da4a2   44 years ago         163MB
    ```
    The size of `distroless-java-base.dynamic-optimized` container is cut down from **164MB** to **130MB**. 
    This is because the native executable reduced in size.

6. Check the size of the native executable:

    ```bash
    ls -lh target/webserver*
    ```

    The expected output is:

    ```bash
    -rwxr-xr-x. 1 opc opc 125M Aug 29 10:51 target/webserver
    ...
    -rwxr-xr-x. 1 opc opc  92M Aug 29 11:06 target/webserver.dynamic-optimized
    ```

    The size decreased from **125M** (`webserver`) to **92M** (`webserver.dynamic-optimized`) by applying the file size optimization.

## **STEP 5**: Build a Size-Optimized Mostly Static Native Image Locally and Run Inside a Container

In this step, you will build a **mostly static** native image, with the file size optimization on, on a host machine, then package it into a container image that provides `glibc`, and run.

### Explanation

A **mostly static** native image links all the shared libraries on which it relies (`zlib`, JDK-shared static libraries) except the standard C library, `libc`. This type of native image is useful for deployment on a distroless base container image.
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
    REPOSITORY   TAG                                      IMAGE ID       CREATED              SIZE
    webserver    distroless-base.mostly-static            b49eec5bdfa6   About a minute ago   117MB
    webserver    distroless-java-base.dynamic-optimized   0bed48dadd11   9 minutes ago        130MB
    webserver    distroless-java-base.dynamic             5a5de47579ef   24 minutes ago       164MB
    webserver    distroless-java-base.jlink               687f7683ad58   About an hour ago    192MB
    webserver    debian-slim.jar                          5c69f06a3972   2 hours ago          238MB
    webserver    0.0.1-SNAPSHOT                           0660806da4a2   44 years ago         163MB
    ```

    The size of the new _distroless-base.mostly-static_ container is **117MB**.
    The reduction in size is related to the fact that a smaller base image was pulled: **gcr.io/distroless/base-debian12**.
    [Distroless images](https://github.com/GoogleContainerTools/distroless) are very small, and the one used is only **48.3 MB**. 
    That's about 40% of the size of **java-base-debian12**(124 MB) used before, and about 25% of the size of **java21-debian12** (192 MB) containing a full JDK.

6. Check the size of the native executable:

    ```bash
    ls -lh target/webserver*
    ```

    The expected output is:

    ```bash
    -rwxrwxr-x. 1 opc opc 124M Oct  8 06:13 target/webserver
    ...
    -rwxrwxr-x. 1 opc opc  92M Oct  8 06:19 target/webserver.dynamic-optimized
    -rwxrwxr-x. 1 opc opc  93M Oct  8 06:29 target/webserver.mostly-static
    ```

    The size of the mostly static native image (`webserver.mostly-static`) has not changed much, and is around **93MB**.
 
## **STEP 6**: Build a Size-Optimized Fully Static Native Image Locally and Run Inside a Container

In this step, you will build a **fully static** native image, with the file size optimization on, on a host machine, then package it into a _scratch_ container.

### Explanation

A **fully static** native image is a statically linked binary that you can use without any additional library dependencies.
You can create a static native image by statically linking it against `musl-libc`, a lightweight, fast, and simple `libc` implementation.
To build a fully static executable, pass the `--static --libc=musl` options at build time.

This static native image is easy to deploy on a slim or distroless container, even a [_scratch_ container](https://hub.docker.com/_/scratch). 
A _scratch_ container is a [Docker official image](https://hub.docker.com/_/scratch), useful for building super minimal images.

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

1. This step requires the `musl` toolchain with `zlib`. Run the following script to download and configure the `musl` toolchain, and install `zlib` into the toolchain:
    ```bash
    ./setup-musl.sh
    ```

2. Run the script to build a fully static native executable and package it into a _scratch_ container:
    ```bash
    ./build-static-image.sh
    ```

3. Once the build completes, a container image _scratch.static_ should be available. Run it, mapping the ports:
    ```bash
    docker run --rm -p8080:8080 webserver:scratch.static
    ```
    The container started in **0.035 seconds**.
    As a result you get a tiny container with a fully functional and deployable server application!

4. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

5. Return to the terminal and stop the running container by clicking CTRL+C.

6. Double-check if the native executable that was just created is indeed fully static with `ldd`:
    ```bash
    ldd target/webserver.static
    ```
    You should see "not a dynamic executable" for the response. 
    Which means that the **image does not rely on any libraries in the operating system environment** and can be packaged in the tiniest container! 

7. Now check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                                      IMAGE ID       CREATED             SIZE
    webserver    scratch.static                           6e8b25dacca6   7 minutes ago       96.4MB
    webserver    distroless-base.mostly-static            b49eec5bdfa6   25 minutes ago      117MB
    webserver    distroless-java-base.dynamic-optimized   0bed48dadd11   32 minutes ago      130MB
    webserver    distroless-java-base.dynamic             5a5de47579ef   48 minutes ago      164MB
    webserver    distroless-java-base.jlink               687f7683ad58   About an hour ago   192MB
    webserver    debian-slim.jar                          5c69f06a3972   3 hours ago         238MB
    webserver    0.0.1-SNAPSHOT                           0660806da4a2   44 years ago        163MB
    ```
    The container size has reduced to **96.4MB**! A _scratch_ container is only **14.5MB**.

8. Check the size of the native executable:

    ```bash
    ls -lh target/webserver*
    ```

    The expected output is:

    ```bash
    -rwxrwxr-x. 1 opc opc 124M Oct  8 06:13 target/webserver
    ...
    -rwxrwxr-x. 1 opc opc  92M Oct  8 06:19 target/webserver.dynamic-optimized
    -rwxrwxr-x. 1 opc opc  93M Oct  8 06:29 target/webserver.mostly-static
    -rwxrwxr-x. 1 opc opc  93M Oct  8 06:54 target/webserver.static
    ```

    The size of the mostly static native image (`webserver.static`) has not changed, and is around **93MB**.

## **STEP 7**: Compress a Static Native Image with UPX and Run Inside a Container

_What can you do next to reduce the size even more?_

You can compress your fully static native image with UPX, then package into the same _scratch_ container, and run.

### Explanation

[UPX](https://upx.github.io/) - an advanced executable file compressor. 
It can significantly reduce the executable size, but note, that UPX loads the executable into the memory, unpacks it, and then recompresses it.

1. Download and install UPX:
    ```bash
    ./setup-upx.sh
    ```

2. Run the script to compress the fully static executable, created at the previous step, and package it into a _scratch_ container.
    ```bash
    ./build-static-upx-image.sh
    ```

3. Once the build completes, a container image _scratch.static-upx_ should be available. Run it, mapping the ports:

    ```bash
    docker run --rm -p8080:8080 webserver:scratch.static-upx
    ```
    The container started in **0.035 seconds**.

4. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

5. Return to the terminal and stop the running container by clicking CTRL+C.

6. Now check how much `upx` compressed the static native image:
    ```bash
    ls -lh target/webserver*
    ```
    The expected output is:
    ```
    -rwxr-xr-x. 1 opc opc 125M Aug 29 10:51 target/webserver
    ...
    -rwxr-xr-x. 1 opc opc  92M Aug 29 11:06 target/webserver.dynamic-optimized
    -rwxr-xr-x. 1 opc opc  92M Aug 29 11:14 target/webserver.mostly-static
    -rwxr-xr-x. 1 opc opc  92M Aug 29 11:32 target/webserver.static
    -rwxr-xr-x. 1 opc opc  35M Aug 29 11:32 target/webserver.static-upx
    ```
    The `upx`-compressed executable (`webserver.static-upx`) is only **35MB**, which is **57MB** less than the "uncompressed" one.

7. Lastly, check the size of all container images:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                                      IMAGE ID       CREATED             SIZE
    webserver    scratch.static-upx                       c87bfe44c7fb   6 seconds ago       36.2MB
    webserver    scratch.static                           6e8b25dacca6   7 minutes ago       96.4MB
    webserver    distroless-base.mostly-static            b49eec5bdfa6   25 minutes ago      117MB
    webserver    distroless-java-base.dynamic-optimized   0bed48dadd11   32 minutes ago      130MB
    webserver    distroless-java-base.dynamic             5a5de47579ef   48 minutes ago      164MB
    webserver    distroless-java-base.jlink               687f7683ad58   About an hour ago   192MB
    webserver    debian-slim.jar                          5c69f06a3972   3 hours ago         238MB
    webserver    0.0.1-SNAPSHOT                           0660806da4a2   44 years ago        163MB
    ```
    The container size reduced dramatically to just **36.2MB**.
    The application and container image's size have been shrunk to the minimum.

## **STEP 8**: (Optional) Build and Run a Native Image Inside a Container Using Paketo Buildpacks

You can also compile this Spring Boot application ahead of time with GraalVM Native Image and run it using Paketo Buildpacks container images. 

> Prerequisite: [GraalVM for JDK 21](https://www.graalvm.org/downloads/). We recommend using [SDKMAN!](https://sdkman.io/). (For other download options, see [GraalVM Downloads](https://www.graalvm.org/downloads/).)
    ```bash
    sdk install java 21-graal
    ```

### Explanation

Spring Boot supports building a native image in a container using the [Paketo Buildpack for Oracle](https://github.com/paketo-buildpacks/oracle) which provides Oracle GraalVM Native Image. 

The Paketo builder pulls the [Jammy Tiny Stack image](https://github.com/paketo-buildpacks/builder-jammy-tiny) (Ubuntu distroless-like image) which contains no buildpacks. 
Then you point the **builder** image to the **creator** image. 
For this workshop, you point to the [Paketo Buildpack for Oracle](https://github.com/paketo-buildpacks/oracle), explicitly requesting the Native Image tool.

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
    docker run --rm -p8080:8080 docker.io/library/webserver:0.0.1-SNAPSHOT
    ```
    
    The application is running from the native image inside a container. The container started in **0.031 seconds**.

3. Open a browser and navigate to [localhost:8080/](http://localhost:8080/). You see the GraalVM documentation pages served.

4. Return to the terminal and stop the running container by clicking CTRL+C.

5. Check the size of this container image:
    ```bash
    docker images
    ```
    The expected output is:
    ```
    REPOSITORY   TAG                          IMAGE ID       CREATED          SIZE
    webserver    0.0.1-SNAPSHOT               0660806da4a2   44 years ago     163MB
    ```
    The new container, tagged as _0.0.1-SNAPSHOT_, is **163MB**.

## **STEP 9**: (Optional) Clean up

To clean up all images, run the `./clean.sh` script provided for that purpose. 

## Conclusions

A fully functional and, at the same time, minimal, Java application was compiled into a native Linux executable and packaged into base, distroless, and scratch containers thanks to GraalVM Native Image's support for various linking options.
All the versions of this Spring Boot application are functionally equivalent.

Sorted by size, it is clear that the fully static native image, compressed with `upx`, and then packaged on the _scratch_ container is the smallest at just **36.2MB**. Note that the website static pages added 44M to the container images size. Static resources are "baked” into native images.

| Container                              | Size of a build artefact <br> (JAR, Jlink runtime, native executable) | Base image | Container |
|----------------------------------------|-----------------------------------------------------------------------|------------|-----------|
| debian-slim.jar                        | webserver-0.0.1-SNAPSHOT.jar **44MB**                                 | 192 MB     | 249MB     |
| distroless-java-base.jlink             | jlink-jre custom runtime **68MB**                                     | 128 MB     | 202MB     |
| distroless-java-base.dynamic           | webserver **125MB**                                                   | 128 MB     | 164MB     |
| 0.0.1-SNAPSHOT                         |                                                                       |            | 163MB     |
| distroless-java-base.dynamic-optimized | webserver-optimized **92MB**                                          | 128 MB     | 130MB     |
| distroless-base.mostly-static          | webserver.mostly-static **92MB**                                      | 48.3 MB    | 117MB     |
| scratch.static                         | webserver.scratch.static **92MB**                                     | 14.5 MB    | 96.4MB    |
| scratch.static-upx                     | webserver.scratch.static-upx **35MB**                                 | 14.5 MB    | 36.2MB    |

## Learn More

- ["Distroless" Container Images](https://github.com/GoogleContainerTools/distroless)
- [Paketo Buildpacks](https://paketo.io/docs/)
- [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html)
- [Static and Mostly Static Images](https://www.graalvm.org/jdk23/reference-manual/native-image/guides/build-static-executables/)
- [Tiny Java Containers by Shaun Smith at DevoxxUK 2022](https://youtu.be/6wYrAtngIVo)