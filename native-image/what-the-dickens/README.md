# Understanding GraalVM Native Image and Containerization

What do Micronaut, Spring Native, Quarkus and Helidon have in common?
They all support [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) - ahead-of-time (AOT) compilation to transform a Java application into a native executable that starts almost instantaneously, provides peak performance with no warmup, and requires less memory and less CPU.

It's perfect for your containerized workloads and microservices where it's critical to minimize startup time and reduce resources consumption.
In this workshop, we provide a practical introduction to GraalVM Native Image, covering how it works, what it can do, and when to use it.

### Objectives

During the workshop you will:
* Build, run, and convert a basic Spring Boot application into a native executable.
* Add the native Spring Boot application to a container image, and deploy it.
* Shrink the size of the container image using Distroless base containers.
* See how to use [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/) as part of your CI/CD pipeline.

Estimated workshop time: 60 minutes

## Prerequisites

Before starting this workshop, you must have installed:

* [GraalVM for JDK 24](https://www.graalvm.org/downloads/) - you can use either the Community or Enterprise Edition.
* A Docker-API compatible container runtime such as [Rancher Desktop](https://docs.rancherdesktop.io/getting-started/installation/), [Docker](https://www.docker.io/gettingstarted/), or [Podman](https://podman.io/getting-started/installation).

## **STEP 1**: Introducing the Sample Java Application

In this workshop you are going to run a sample application with a minimal REST-based API.

The source code, resources, and build scripts for the application are in _native-image/what-the-dickens/lab/src_.
The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework.

The application has two classes:

* `com.example.DickensApplication`: The main Spring Boot class.
* `com.example.DickensController`: A REST controller class that implements the logic of the application and defines the HTTP endpoints `/whatTheDickens` and `/whatTheDickens/{number}`.

So, what does the application do? If you call the endpoint `/whatTheDickens`, it will return 10 lines of nonsense prose generated
in the style of [Charles Dickens' novels](https://en.wikipedia.org/wiki/Charles_Dickens#Novels).
The application achieves this by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to produce a statistical model of Dickens' original prose.
This model is then used to generate new text.

The example application ingests the text of four of Dickens' novels (provided in _main/resources/_), from which it creates a model.
The application then uses the model to generate new text that is similar to the original text.
The [RiTa library](https://rednoise.org/rita/) does the heavy lifting for you--it provides the functionality to build and use Markov Chains.

Below are two snippets from class `com.example.DickensController`.

- The first snippet shows how the model is created and then populated with `prose` ingested from some of Dickens' novels.
The model is created and populated in the class initializer.

    ```java
    private final static RiMarkov MARKOV_MODEL = new RiMarkov(5);
    ...
    MARKOV_MODEL.addText(prose);
    ```

- In the second snippet, you can see the method that generates new lines of prose from the model.

    ```java
    public String generate(final int numLines) {
      String[] lines = MARKOV_MODEL.generate(numLines, GEN_MAP);
      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        sb.append("<p>");
        sb.append(line);
        sb.append("</p>");
      }
      return sb.toString();
    }
    ```


### Action

1. Build the application from the root directory of the project:

    ```bash
    cd native-image/what-the-dickens/lab
    ./mvnw clean package
    ```

    This will generate an runnable JAR file, one that contains all the application's dependencies as well as a correctly configured _MANIFEST_ file.

2. Run this JAR file as follows:

    ```bash
    java -jar ./target/what_the_dickens-0.0.1-SNAPSHOT.jar &
    ```
    It runs the Java application in the background.
    When the application starts, you should see something similar to the following:
    ```
    Started DickensApplication in 2.768 seconds (process running for ...)
    ```

3. Use the `curl` command to call the application's endpoints to see what the application returns.

    ```bash
    curl http://localhost:8080/whatTheDickens
    curl http://localhost:8080/whatTheDickens/30
    ```

    Did you get the some nonsense sentences returned from the application?

4. Before moving on to the next step, bring the application to the foreground by typing `fg`, and  then stop the application with Ctrl-C.

## **STEP 2**: Building a Native Executable

Now you can create a native executable from your Java application using GraalVM Native Image.
The native executable is going to exhibit two interesting characteristics, namely:

- It is going to start really fast.
- It will use fewer resources than its corresponding Java application.

You can use the `native-image` tool installed with GraalVM on the command line, but, since you are using Maven already, you better apply the
[Maven Plugin for Native Image](https://graalvm.github.io/native-build-tools/latest/end-to-end-maven-guide.html) which conveniently enables you to carry on using Maven.

One way of building a native executable is to use a [Maven profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html),
which enables you to decide whether you want to build the JAR file or the native executable.

The Maven _pom.xml_ file contains a [profile that builds a native executable](https://graalvm.github.io/native-build-tools/latest/end-to-end-maven-guide.html#add-plugin).
Take a closer look:

```xml
<profiles>
    <profile>
        <id>native</id>
        <!-- Rest of profile hidden, to highlight relevant parts -->
    </profile>
</profiles>
```

Next, within the profile, the `native-maven-plugin` is included and attached to the Maven `package` phase.
This means it will run as a part of the `package` phase.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.graalvm.buildtools</groupId>
      <artifactId>native-maven-plugin</artifactId>
      <extensions>true</extensions>
      <executions>
        <execution>
          <id>build-native</id>
          <phase>package</phase>
          <goals>
              <goal>compile-no-fork</goal>
          </goals>
        </execution>
      </executions>
      <!-- Rest of profile hidden, to highlight relevant parts -->
    </plugin>
  </plugins>
</build>
```

The Native Image tool relies on the static analysis of an application’s reachable code at runtime.
However, the analysis cannot always completely predict all usages of the Java Native Interface (JNI), Java Reflection, Dynamic Proxy objects, or class path resources.
Undetected usages of these dynamic features must be provided to Native Image in the form of JSON configuration.
GraalVM provides a [Tracing agent](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/) to easily gather metadata and create configuration for you.

The agent tracks all usages of dynamic features during application execution on a Java VM.
Spring should generate most of this configuration automatically, but the tracing agent can be used to quickly identify missing entries.

### Action

1. Use the command below to launch the Spring application with the Native Image tracing agent attached.

    ```bash
    java -Dpring.aot.enabled=true \
    -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/ \
    -jar target/what_the_dickens-0.0.1-SNAPSHOT.jar
    ```

2. Stop the application with Ctrl-C.

    On application shutdown, the tracing agent writes the configuration file _reachability-metadata.json_ to the specified output directory: _src/main/resources/META-INF/native-image/_.
    In this case, it's the default directory that Native Image expects to find the configuration file.

3. Now run the Maven build using the profile, as below (note that the profile name is specified with the `-P` option, and you can skip the tests from now on):

    ```bash
    ./mvnw -Pnative -DskipTests=true package
    ```
    This generates a native executable in the _target_ directory, named _what\_the\_dickens_.

4. Run this native executable in the background:

    ```bash
    ./target/what_the_dickens &
    ```
    When the application starts, you should see something similar to the following:
    ```
    Started DickensApplication in 2.86 seconds (process running for ...)
    ```

5. Test it, using the following commands:

    ```bash
    curl http://localhost:8080/whatTheDickens
    curl http://localhost:8080/whatTheDickens/30
    ```

    Now you have a native executable of your application--however, it starts no faster than the original Java version.
    This is mainly because the performance is constrained by the amount of time the application takes to ingest the Dickens' novels.

6. Stop the application before you move on as before (type `fg` and then Ctrl-C).

## **STEP 3**: Reducing the Startup Time of Your Native Executable

Notice that you can pass configuration options and parameters to the underlying `native-image` tool using the `buildArgs` element of the _pom.xml_ file.
In individual `buildArg` elements you can pass in options in exactly the same way as on the command line.
One of those is the option to [initialize some application classes explicitly](https://www.graalvm.org/latest/reference-manual/native-image/guides/specify-class-initialization/) using the `initialize-at-build-time` option.

### Action
1. Edit the `native` profile of the _pom.xml_ file so that passes in the `initialize-at-build-time` option as shown below:

    ```xml
    <configuration>
        <buildArgs>
            <buildArg>--initialize-at-build-time=com.example.DickensController,rita.RiMarkov,rita.Util,rita.Tokenizer,rita.RiTa</buildArg>
        </buildArgs>
        <!-- Rest of profile hidden, to highlight relevant parts -->
    </configuration>
    ```
    This causes the static initializer of classes `com.example.DickensController` and several of the classes in the `rita` package to be run when `native-image` builds the native executable.

2. Now run the Maven build using the profile again, as below:

    ```bash
    ./mvnw -Pnative -DskipTests=true package
    ```

    > Note: Depending on your platform, this can take many minutes. To reduce the time, reduce the number of ingested novels in `com.example.DickensController`,
    increase the memory dedicated to Native Image, or use the `quickBuild` option (see _pom.xml_).

    This will generate a new native executable in the _target_ directory, again named _What\_the\_Dickens_.

3. Run this native executable in the background:

    ```bash
    ./target/what_the_dickens &
    ```

    When the application starts, you should see something similar to the following:
    ```
    Started DickensApplication in 0.081 seconds (process running for 0.123)
    ```

4. Test it, using the following commands:

    ```bash
    curl http://localhost:8080/whatTheDickens
    curl http://localhost:8080/whatTheDickens/30
    ```

    Now you have a native executable of your application that starts much faster than your original Java version!

5. Stop the application before you move on as before (type `fg` and then Ctrl-C).

## **STEP 4**: Containerizing your Native Executable

So you have a native executable version of your application, and you have seen it working. Now containerize it.

A Dockerfile for packaging this native executable is provided for you in _native-image/what-the-dickens/lab/01-native-image/Dockerfile_.
The contents are shown below, along with explanatory comments.
This is a two-step build: the first part builds the native executable, using GraalVM Native Image;
the second step copies the native executable into a deployment container.

>    ### Note: Why Are We Using a Two-Step Build?
>
>    The native executable produced by GraalVM Native Image is compatible with the platform on which you run it.
>
>    If you are running on Linux, it will generate an ELF Linux executable (for the architecture of the chip you are running on).
>    If you want to containerize your application in a container and you are running on macOS,
>    then you can't simply build the executable locally (as that will be a macOS-compatible executable) and package it
>    within your container which expects the executable to be Linux-compatible.
>
>    Therefore, if you are developing on any OS other than Linux, you need to first build within a container using a
>    Linux version of GraalVM to create an executable, and it can then be packaged into another container.
>    This is what the multi-stage Docker build here achieves.
>    The first step in the process builds a Linux-compatible executable and the second step packages that into a container image for deployment.

```dockerfile
# Base Container Image
FROM container-registry.oracle.com/graalvm/native-image:24 AS builder

# Set the working directory to /build
WORKDIR /build

# Copy the source code into the container for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress -Pnative -DskipTests=true clean package

# The deployment container image
FROM docker.io/oraclelinux:8-slim

# This container will expose TCP port 8080, as this is the port on which your app will listen
EXPOSE 8080

# Copy the native executable into the container
COPY --from=builder /build/target/what_the_dickens .

# Run what_the_dickens when starting the container
ENTRYPOINT ["/what_the_dickens"]
```

> ### Note: Building on Linux
> If you are using Linux, you don't need to use a multi-stage build and your build times will be faster.
> You can just build the native executable locally and package it in the deployment container _01-native-image/Dockerfile.linux_
> as follows:
> ```bash
> # Build the native executable again if anything has changed
> ./mvnw -Pnative -DskipTests=true clean package
> docker build -f ./01-native-image/Dockerfile.linux --build-arg APP_FILE=target/what_the_dickenss -t what_the_dickens:native.01 .
> ```

### Action

1. To build a container image, run the following commands.

  - First clean the development directory:
    ```bash
    ./mvnw clean
    ```
  - Now build a container:
    ```bash
    docker build -f ./01-native-image/Dockerfile \
                -t what_the_dickens:native.01 .
    ```
  - List the newly built container image
    ```bash
    docker images | head -n2
    ```

    > Note: Depending on your platform, this can take many minutes. To reduce the time, reduce the number of ingested novels in `com.example.DickensController`,
    increase the memory dedicated to Native Image, or use the `quickBuild` option (see _pom.xml_).

2. Finally, run the container image:

    ```bash
    docker run --rm --name "what_the_dickens-native" -p 8080:8080 what_the_dickens:native.01
    ```

3. When the Spring application has started, test it as before:

    ```bash
    curl http://localhost:8080/whatTheDickens
    curl http://localhost:8080/whatTheDickens/30
    ```

    Again, you should see more nonsense prose in the style of the Dickens' novels.

    You can see how long the application took to start by inspecting the output from the container.
    It should be around 0.396s.

4. Before you go to the next step, take a look at the size of the container produced:

    ```bash
    size_in_bytes=`docker inspect -f "{{ .Size }}" what_the_dickens:native.01`
    echo $((size_in_bytes/1024/1024))
    ```

    The container image size we saw in our experiments was 562MB.

5. Stop your container (using Ctrl+C) before moving onto the next step.

## **STEP 5**: Building a Mostly-Static Executable and Packaging it in a "Distroless" Container Image

Let's recap, again, what you have done:

1. You have built a Spring Boot application with a HTTP endpoint, `/whatTheDickens`.
2. You have built a native executable of your application using the Maven plugin for Native Image.
3. You have containerized your native executable.

In this step, you will shrink your container size even further--smaller containers are quicker to download and start.
With GraalVM Native Image you have the ability to statically link system libraries into the native executable.
If you build a statically linked native executable, you can package the native executable directly into an empty container image, also known as a "scratch" container.

Another option is to produce what is known as a mostly-statically linked native executable.
With this, you statically link in all system libraries except for the standard C library, `glibc`.
With such a native executable you can use a small container, such as Google's "Distroless" which contains the `glibc` library, some standard files, and SSL security certificates.

In this step, you will build a mostly-statically linked executable and then package it into a Distroless container.

A Dockerfile for packaging this native executable is in the directory _native-image/what-the-dickens/lab/02-smaller-containers/Dockerfile_.
The contents are shown below, along with explanatory comments.
As earlier, this is a two-step build: the first part builds the native executable, using GraalVM Native Image;
the second step copies the native executable into a Distroless container.

```dockerfile
# Base Container Image
FROM container-registry.oracle.com/graalvm/native-image:24 AS builder

# Set the working directory to /build
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN ./mvnw --no-transfer-progress -Pnative -DskipTests=true clean package

# Deployment Container Image
# This time we use the distroless image - which is around 20 MB in size. Even smaller versions are available.
FROM gcr.io/distroless/base

# This container will expose TCP port 8080, as this is the port on which your app will listen
EXPOSE 8080

# Copy the native executable into the container
COPY --from=builder /build/target/what_the_dickens .

# Run what_the_dickens when starting the container
ENTRYPOINT ["/what_the_dickens"]
```

> ### Note: Building on Linux
> If you are using Linux, you don't need to use a multi-stage build and your build times will be faster.
> You can just build the native executable locally and package it in the deployment container _02-smaller-containers/Dockerfile.linux_
> as follows:
> ```bash
> # Build the native executable again if anything has changed
> ./mvnw -Pnative -DskipTests=true clean package
> docker build -f ./02-smaller-containers/Dockerfile.linux --build-arg APP_FILE=target/what_the_dickens -t what_the_dickens:distroless.01 .
> ```

### Action

1. Edit the `native` profile of the _pom.xml_ file so that also passes in the `--static-nolibc` option as shown below.

    ```xml
    <configuration>
        <buildArgs>
            <buildArg>--static-nolibc</buildArg>
            <!-- Rest of profile hidden, to highlight relevant parts -->
        </buildArgs>
    </configuration>
    ```
    This instructs Native Image to build a mostly-statically linked native executable.

2. To build a container image, run the following command:

    ```bash
    docker build -f ./02-smaller-containers/Dockerfile \
                -t what_the_dickens:distroless.01 .
    ```

    List the newly built image:
    ```bash
    docker images | head -n2
    ```

    > Note: Depending on your platform, this can take many minutes. To reduce the time, reduce the number of ingested novels in `com.example.DickensController`,
    increase the memory dedicated to Native Image, or use the `quickBuild` option (see _pom.xml_).

3. Now run the container image:

    ```shell
    docker run --rm --name "what_the_dickens-distroless" -p 8080:8080 what_the_dickens:distroless.01
    ```

4. When the Spring application has started, test it as before:

    ```bash
    curl http://localhost:8080/whatTheDickens
    curl http://localhost:8080/whatTheDickens/30
    ```

    You can see how long the application took to startup by inspecting the output from the container.
    In our experiments, the native executable started up in 0.31s (compared to 0.396s for the original container image in **Step 4**).

    Great! It worked. But how small, or large, is your container?

5. Use the following commands to check the image size:

    ```shell
    size_in_bytes=`docker inspect -f "{{ .Size }}" what_the_dickens:distroless.01`
    echo $((size_in_bytes/1024/1024))
    ```

In our experiments, we saw a size of around 484MB, compared to 562MB for an Oracle Linux 8 container image. So, you have shrunk the container significantly!

## Summary

We hope you have enjoyed this workshop and learnt a few things along the way.
You have seen how to convert a Java application into a native executable, which starts significantly faster than the Java application; and how to containerize that native executable.

Finally, you looked at how to build a mostly-statically linked native executable.
Such an executable can be packaged in smaller Distroless containers, which enables you to significantly reduce the size of the container!

### Learn More

- [Static and Mostly Static Images](https://www.graalvm.org/jdk24/reference-manual/native-image/guides/build-static-executables/)
- [Tiny Java Containers](https://github.com/graalvm/graalvm-demos/tree/master/native-image/tiny-java-containers)
- [Native Build Tools](https://graalvm.github.io/native-build-tools/latest/index.html)
- [Improving Java Application Security with Practical Hardening Strategies by Shaun Smith at DevoxxUK 2024](https://www.youtube.com/watch?v=dBbYnVSTwQs)