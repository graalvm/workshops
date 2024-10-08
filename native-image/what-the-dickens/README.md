<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg"
alt="GraalVM logo"
width="200px">

What do Micronaut, Spring Native, Quarkus and Helidon have in common?
They all support [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/latest/docs/reference-manual/native-image/) ahead-of-time (AOT) compilation to transform a Java application into a native executable that starts almost instantaneously, provides peak performance with no warmup, and requires less memory and less CPU.

It's perfect for your containerised workloads and microservices where it's critical to minimise your startup time and reduce your resource consumption.
In this lab we'll provide a practical introduction to GraalVM Native Image AOT covering how it works, what it can do, and when to use it.

### Lab Objectives

By the end of the lab you will have:
* Built, run and converted a basic Spring Boot application into a native executable
* Added the native Spring Boot application to a container image, deployed it and run it
* Shrunk the size of the container image using Distroless containers
* Seen how to use GraalVM Native Build tools as part of your CI/CD pipeline

Estimated lab time: 60 minutes

# Understanding GraalVM Native Image and Containerisation

## Introduction

This lab takes you step by step through the process of how to containerise GraalVM Native Image applications.

GraalVM Native Image technology compiles Java code ahead-of-time into a native executable file.
Only the code that is required at run time by the application is included in the executable file.

An executable file produced by Native Image has several important advantages, in that it:

- Uses a fraction of the resources required by the JVM, so is cheaper to run
- Starts in milliseconds
- Delivers peak performance immediately, with no warmup
- Can be packaged into a lightweight container image for faster and more efficient deployment
- Presents a reduced attack surface (more on this in future labs)

Many of the leading microservice frameworks support ahead-of-time compilation with GraalVM Native Image, including
Micronaut, Spring, Helidon, and Quarkus.

In addition, there are Maven and Gradle plugins for Native Image so you can easily build,
test, and run Java applications as executable files.

(Oracle Cloud Infrastructure (OCI) provides GraalVM Enterprise at no additional cost.)

>Note: If you see the laptop icon in the lab, this means you need to do something such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## Lab Prerequisites

Before starting this lab, you must have installed:

* [GraalVM Installation 22 or later, JDK17 +](https://github.com/graalvm/get.graalvm.org/) - We recommend the Enterprise Edition
* The `native-image` tool (see [Native Image](https://www.graalvm.org/latest/docs/getting-started/#native-image))
* Set your `JAVA_HOME` environment variable to point to your GraalVM installation
* Maven 3.0 or above
* A Docker-API compatible container runtime such as [Rancher Desktop](https://docs.rancherdesktop.io/getting-started/installation/), [Docker](https://www.docker.io/gettingstarted/), or [Podman](https://podman.io/getting-started/installation)

## **STEP 1**: Introducing the Sample Java Application

In this lab you are going to build a sample application with a minimal REST-based API.
You are then going to containerise the application.
First, take a quick look at the sample application.

The source code, resources, and build scripts for the application are in _native-image/what-the-dickens/lab/src_.
The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework.

The application has two classes:

* `com.example.DickensApplication`: The main Spring Boot class
* `com.example.DickensController`: A REST controller class that implements the logic of the application and defines the HTTP endpoints `/whatTheDickens` and `/whatTheDickens/{number}`

So, what does the application do? If you call the endpoint `/whatTheDickens`, it will return 10 lines of nonsense prose generated
in the style of [Charles Dickens' novels](https://en.wikipedia.org/wiki/Charles_Dickens#Novels).
The application achieves this by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to produce a statistical model of Dickens' original prose.
This model is then used to generate new text.

The example application ingests the text of four of Dickens' novels (provided in _main/resources/_), from which it creates a model.
The application then uses the model to generate new text that is similar to the original text.
The [RiTa](https://rednoise.org/rita/) library does the heavy lifting for you--it provides the functionality to build and use Markov Chains.

Below are two snippets from class `com.example.DickensController`.

1. The first snippet shows how the model is created and then populated with `prose` ingested from some of Dickens' novels.
The model is created and populated in the class initializer.

    ```java
    private final static RiMarkov MARKOV_MODEL = new RiMarkov(5);
    ...
    MARKOV_MODEL.addText(prose);
    ```

2. In the second snippet you can see the method that generates new lines of prose from the model.

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

Take a little while to view the code and acquaint yourself with it.

Build your application: from the root directory of the project, run the following commands:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
cd native-image/what-the-dickens/lab
./mvnw clean package
```

This will generate an "executable" JAR file, one that contains all the application's dependencies as well as a correctly configured _MANIFEST_
file.
You can run this JAR file as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Run the Java application in the background
java -jar ./target/What_the_Dickens-0.0.1-SNAPSHOT.jar &
```

When the application starts, you should see something similar to the following:

```
Started DickensApplication in 8.845 seconds (process running for 9.598)
```

Use the `curl` command to call the application's endpoints to see what the application returns.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Call the endpoints
curl http://localhost:8080/whatTheDickens
curl http://localhost:8080/whatTheDickens/30
```

Did you get the some nonsense sentences returned from the application? 

Before moving on to the next step, stop the application with Ctrl-C.

## **STEP 2**: Building a Native Executable

Now you can create a native executable from your Java application using GraalVM Native Image.
The native executable is going to exhibit two interesting characteristics, namely:

1. It is going to start really fast
2. It will use fewer resources than its corresponding Java application

You can use the Native Image tooling installed with GraalVM to build a native executable of a Java
application from the command line, but as you are using Maven already, you are going to use the 
[GraalVM Native Image Build Tools for Maven](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) which will
conveniently allow you to carry on using Maven.

One way of building a native executable is to use a Maven [profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html), 
which will allow you to decide whether you want to build the JAR file or the native executable.

The Maven _pom.xml_ file contains a profile that builds a native executable.
(For more details see [Registering the plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html#configuration-registering-plugin).)
Take a closer look:

The profile is declared and given a name.

```xml
<profiles>
    <profile>
        <id>native</id>
        <!-- Rest of profile hidden, to highlight relevant parts -->
    </profile>
</profiles>
```

Next, within the profile, the GraalVM Native Image Build Tools plugin is included and attached to the `package` phase in Maven.
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
Undetected usages of these dynamic features must be provided to Native Image in the form of metadata (precomputed in code or as JSON configuration files).
GraalVM provides a [Tracing Agent](https://docs.oracle.com/graalvm/enterprise/22/docs/reference-manual/native-image/metadata/AutomaticMetadataCollection/) to easily gather metadata and create configuration files.
The agent tracks all usages of dynamic features during application execution on a regular Java VM.
Spring should generate most of this configuration automatically, but the tracing agent can be used to quickly identify missing entries.

Use the command below to launch the Spring application with the Native Image tracing agent attached.
(For more information, see [Using the Tracing Agent](https://docs.spring.io/spring-boot/docs/3.0.5/reference/htmlsingle/#native-image.advanced.using-the-tracing-agent).)

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java -Dpring.aot.enabled=true \
-agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image/ \
-jar target/What_the_Dickens-0.0.1-SNAPSHOT.jar
```

Stop the application with Ctrl-C.

On application shutdown the tracing agent writes the configuration files to the specified output directory: _src/main/resources/META-INF/native-image/_.
In this case, it's the default directory that Native Image expects to find configuration files.

Now run the Maven build using the profile, as below (note that the profile name is specified with the `-P` option, and you can skip the tests from now on):

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./mvnw -Pnative -DskipTests=true clean package
```

> Note: Depending on your platform, this can take several minutes.

This will generate a native executable in the _target_ directory, named _What\_the\_Dickens_.

Run this native executable in the background:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./target/What_the_Dickens &
```

When the application starts, you should see something similar to the following:

```
Started DickensApplication in 12.829 seconds (process running for 12.925)
```

Test it, using the following commands:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
curl http://localhost:8080/What_the_Dickens
curl http://localhost:8080/What_the_Dickens/30
```

Now you have a native executable of your application--however, it starts no faster than the original Java version.
This is mainly because the performance is constrained by the amount of time the application takes to ingest the Dickens' novels. 

Stop the application before you move on, using Ctrl-C.

## **STEP 3**: Reducing the Startup Time of Your Native Executable

Notice that you can pass configuration options and parameters to the underlying Native Image
build tool using the `buildArgs` element of the _pom.xml_ file.
In individual `buildArg` elements you can pass in options in exactly the same way
as you do to Native Image, so you can use all of the options that Native Image accepts.
One of those is the option to [Specify Class Initialization Explicitly](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/guides/specify-class-initialization/) using the `initialize-at-build-time` option.

Edit the `native` profile of the _pom.xml_ file so that passes in the `initialize-at-build-time` option as shown below:

```xml
<configuration>
    <buildArgs>
        <buildArg>--initialize-at-build-time=com.example.DickensController,rita.RiMarkov,rita.Util,rita.Tokenizer,rita.RiTa</buildArg>
    </buildArgs>
    <!-- Rest of profile hidden, to highlight relevant parts -->
</configuration>
```

This causes the static initializer of classes `com.example.DickensController` and several of the classes in the `rita` package to be run when Native Image builds the native executable.

Now run the Maven build using the profile again, as below:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./mvnw -Pnative -DskipTests=true clean package
```

> Note: Depending on your platform, this can take many minutes. To reduce the time, reduce the number of ingested novels in `com.example.DickensController`, 
increase the memory dedicated to Native Image, or use the `quickBuild` option (see _pom.xml_).
 
This will generate a new native executable in the _target_ directory, again named _What\_the\_Dickens_. The build output should contain something similar to:

```
Apr 12, 2023 4:23:17 PM com.example.DickensController <clinit>
INFO: Time taken to ingest novels:  5,625ms
```

Run this native executable in the background:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./target/What_the_Dickens &
```

When the application starts, you should see something similar to the following:

```
Started DickensApplication in 0.081 seconds (process running for 0.123)
```

Test it, using the following commands:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
curl http://localhost:8080/What_the_Dickens
curl http://localhost:8080/What_the_Dickens/30
```

Now you have a native executable of your application that starts much faster than your original Java version. 

Stop the application before you move on, using Ctrl-C.

## **STEP 4**: Containerising your Native Executable

So you have a native executable version of your application, and you have seen it working. Now to containerise it.

A Dockerfile for packaging this native executable is in _native-image/what-the-dickens/lab/01-native-image/Dockerfile_.
The contents are shown below, along with explanatory comments.
This is a two-step build: the first part builds the native executable, using GraalVM Native Image;
the second step copies the native executable into a deployment container.

>    ### Note: Why Are We Using a Two-Step Build?
>
>    The native executable produced by GraalVM Native Image is compatible with the platform on which you run Native Image.
>    
>    So, if you are running on macOS, it will generate a mach64 executable.
>    If you are running on Linux, it will generate an ELF Linux executable (for the architecture of the chip you are running on).
>    If you want to containerise your application in a container and you are running on macOS, 
>    then you can't simply build the executable locally (as that will be a macOS-compatible executable) and package it 
>    within your container which expects the executable to be Linux-compatible.
> 
>    Therefore, if you are developing on any OS other than Linux, you need to first build within a container using a 
>    Linux version of GraalVM to create an executable that can then be packaged into another container.
>    That is what the multi-stage build here achieves.
>    The first step in the process builds a Linux-compatible executable and the second step packages that into a container image for deployment.

```dockerfile
# Base Container Image
FROM ghcr.io/graalvm/native-image:ol8-java17-22.3.1 AS builder

# Install tar and gzip to extract the Maven binaries
RUN microdnf update \
 && microdnf install --nodocs \
    tar \
    gzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

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
COPY --from=builder /build/target/What_the_Dickens .

# Run What_the_Dickens when starting the container
ENTRYPOINT ["/What_the_Dickens"]
```

> ### Note: Building on Linux
> If you are using Linux, you don't need to use a multi-stage build and your build times will be faster.
> You can just build the native executable locally and package it in the deployment container _01-native-image/Dockerfile.linux_
> as follows:
> ```bash
> # Build the native executable again if anything has changed
> ./mvnw -Pnative -DskipTests=true clean package
> docker build -f ./01-native-image/Dockerfile.linux --build-arg APP_FILE=target/What_the_Dickens -t What_the_Dickens:native.01 .
> ```

To build a container image, run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# First clean the development directory
./mvnw clean
# Build a Container Image
docker build -f ./01-native-image/Dockerfile \
             -t what_the_dickens:native.01 .
# List the newly built container image
docker images | head -n2
```

> Note: Depending on your platform, this can take many minutes. To reduce the time, reduce the number of ingested novels in `com.example.DickensController`,
increase the memory dedicated to Native Image, use the `quickBuild` option (see _pom.xml_), or increase the memory given to your container engine.

And that is it. Use this command to run the container image:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker run --rm --name "what_the_dickens-native" -p 8080:8080 what_the_dickens:native.01
```

When the Spring application has started, test it as before:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
curl http://localhost:8080/whatTheDickens
curl http://localhost:8080/whatTheDickens/30
```

Again, you should see more nonsense prose in the style of the Dickens' novels.

You can see how long the application took to startup by inspecting the output from the container.
In our experiments, the native executable started up in 0.396s

```
2023-04-14T13:59:26.864Z  INFO 1 --- [           main] com.example.DickensApplication           : Started DickensApplication in 0.396 seconds (process running for 0.528)
```

Before you go to the next step, take a look at the size of the container produced:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
size_in_bytes=`docker inspect -f "{{ .Size }}" what_the_dickens:native.01`
echo $((size_in_bytes/1024/1024))
```

The container image size we saw in our experiments was 562MB. 

Stop your container (using Ctrl-C) before moving onto the next step.

## **STEP 5**: Building a Mostly-Static Executable and Packaging it in a "Distroless" Container Image

Let's recap, again, what you have done:

1. You have built a Spring Boot application with a HTTP endpoint, `/whatTheDickens`
2. You have built a native executable of your application using the Native Image build Tools for Maven
3. You have containerised your native executable

In this step, you will shrink your container size even further--smaller containers are quicker to download and start.
With GraalVM Native Image you have the ability to statically link system libraries into the native executable.
If you build a statically linked native executable, you can package the native executable directly into an empty 
container image, also known as a "scratch" container.

Another option is to produce what is known as a mostly-statically linked native executable.
With this, you statically link in all system libraries except for the standard C library, `glibc`.
With such a native executable you can use a small container, such as Google's "Distroless" which contains the `glibc` library, some standard files, and SSL security certificates.
The standard Distroless container is around 20MB in size.

You will build a mostly-statically linked executable and then package it into a Distroless container.

A Dockerfile for packaging this native executable is in the directory
_native-image/what-the-dickens/lab/02-smaller-containers/Dockerfile_.
The contents are shown below, along with explanatory comments.
As earlier, this is a two-step build: the first part builds the native executable, using GraalVM Native Image; 
the second step copies the native executable into a Distroless container.

```dockerfile
# Base Container Image
FROM ghcr.io/graalvm/native-image:ol8-java17-22.3.1 AS builder

# Install tar and gzip to extract the Maven binaries
RUN microdnf update \
 && microdnf install --nodocs \
    tar \
    gzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

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
COPY --from=builder /build/target/What_the_Dickens .

# Run What_the_Dickens when starting the container
ENTRYPOINT ["/What_the_Dickens"]
```

Edit the `native` profile of the _pom.xml_ file so that also passes in the `StaticExecutableWithDynamicLibC` option as shown below:

This instructs Native Image to build a mostly-statically linked native executable.

```xml
<configuration>
    <buildArgs>
        <buildArg>--initialize-at-build-time=com.example.DickensController,rita.RiMarkov,rita.Util,rita.Tokenizer,rita.RiTa</buildArg>
        <buildArg>-H:+StaticExecutableWithDynamicLibC</buildArg>
        <!-- Rest of profile hidden, to highlight relevant parts -->
    </buildArgs>
</configuration>
```

> ### Note: Building on Linux
> If you are using Linux you don't need to use a multi-stage build and your build times will be faster.
> You can just build the native executable locally and package it in the deployment container _02-smaller-containers/Dockerfile.linux_
> as follows:
> ```bash
> # Build the native executable again if anything has changed
> ./mvnw -Pnative -DskipTests=true clean package
> docker build -f ./02-smaller-containers/Dockerfile.linux --build-arg APP_FILE=target/What_the_Dickens -t What_the_Dickens:distroless.01 .
> ```

To build a container image, run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Build a Container Image
docker build -f ./02-smaller-containers/Dockerfile \
             -t what_the_dickens:distroless.01 .
# List the newly built image
docker images | head -n2
```

> Note: Depending on your platform, this can take many minutes. To reduce the time, reduce the number of ingested novels in `com.example.DickensController`,
increase the memory dedicated to Native Image, use the `quickBuild` option (see _pom.xml_), or increase the memory given to your container engine.

Use this command to run the container image:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker run --rm --name "what_the_dickens-distroless" -p 8080:8080 what_the_dickens:distroless.01
```

When the Spring application has started, test it as before:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
curl http://localhost:8080/whatTheDickens
curl http://localhost:8080/whatTheDickens/30
```

You can see how long the application took to startup by inspecting the output from the container.
In our experiments, the native executable started up in 0.31s (compared to 0.396s for the original container image in **Step 4**). A big improvement!

```
2023-04-14T14:21:33.864Z  INFO 1 --- [           main] com.example.DickensApplication           : Started DickensApplication in 0.31 seconds (process running for 0.335)
```

Great! It worked. But how small, or large, is your container? Use the following commands to check the image size:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
size_in_bytes=`docker inspect -f "{{ .Size }}" what_the_dickens:distroless.01`
echo $((size_in_bytes/1024/1024))
```

In our experiments, we saw a size of around 484MB, compared to 562MB for an Oracle Linux 8 
container image. So, you have shrunk the container significantly.

## Summary

We hope you have enjoyed this lab and learnt a few things along the way. We've looked at how you can convert a Java application into a native executable, which starts significantly 
faster than the Java application. You then containerised the native executable.

Finally, we looked at how we can build a mostly-statically linked native executable with Native Image. 
These can be packaged in smaller containers, such as Distroless and these let us shrink the size of the container image even further.

### Learn More

- Watch a presentation by Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
