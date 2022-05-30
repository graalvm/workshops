<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg"
alt="GraalVM logo"
width="200px">

# Understanding Containerisation and GraalVM Native Image

## Introduction

This lab is for developers looking to understand more about how to containerise 
[GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/) applications.

GraalVM Native Image technology compiles Java code ahead-of-time into a native executable file. Only the code that is 
required at run time by the application is included in the executable file.

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

> **Note:** Oracle Cloud Infrastructure (OCI) provides GraalVM Enterprise at no additional cost.

Estimated lab time: 90 minutes

### Lab Objectives

In this lab you will:

- Add a basic Spring Boot application to a Docker Image and run it
- Build a native executable from this application, using GraalVM Native Image 
- Add the native executable to a Docker Image
- Shrink your application docker image size with GraalVM Native Image & Distroless containers 

**NOTE:** If you see the laptop icon in the lab, this means you need to do something such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# This is where you will need to do something
```

## Prerequisites

For this Lab you will need the following:

* [GraalVM Installation 22 or greater, JDK11 +](https://www.graalvm.org/downloads/) - You can use either the Community or Enterprise Edition 
* The `native-image` tool (see [Native Images](https://www.graalvm.org/22.0/docs/getting-started/#native-images))
* Set your `JAVA_HOME` environment variable to point to your GraalVM installation
* Maven 3.0 or above
* [Docker](https://docs.docker.com/get-docker/) or [Podman](https://podman.io/getting-started/installation)

## **STEP 1**: Meet Our Sample Java Application

In this lab you are going to build a simple application with a very minimal REST-based API. You are then going to 
containerise this application, using Docker. First, let's take a quick look at your simple application.

We have provided the source code and build scripts for this application in:

```txt
native-image/containerisation/lab/src/
```

The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework and makes use
of the [Spring Native Project](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/) (a Spring incubator
to generate native executables using GraalVM Native Image).

The application has two classes:

* `com.example.demo.DemoApplication` : The main Spring Boot class, that also defines the HTTP endpoint, `/jibber`
* `com.example.demo.Jabberwocky` : A utility class that implements the logic of the application

So, what does the application do? If you call the endpoint `/jibber`, it will return some nonsense verse generated
in the style of the [Jabberwocky poem](https://en.wikipedia.org/wiki/Jabberwocky), by Lewis Carroll. The program achieves this
by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to model the original poem (this is essentially a statistical model). 
This model generates new text.

In the example application you provide the application with the text of the poem, then generate a model of the text, which the application then uses to 
generate a new text that is similar to the original text. We are using the [RiTa](https://rednoise.org/rita/) library to do the heavy lifting for us--it supports building and using Markov Chains.

Below are two snippets from the utility class `com.example.demo.Jabberwocky`, that builds the model. The `text` variable
contains the text of the original poem. This snippet shows how we create the model and then populate it with `text`.
This is called from the class constructor
and we define the class to be a [Singleton](https://docs.spring.io/spring-framework/docs/3.0.0.M3/reference/html/ch04s04.html#beans-factory-scopes-singleton)
(so only one instance of the class ever gets created).

```java
this.r = new RiMarkov(3);
this.r.addText(text);
```

Here you can see the method to generate new lines of verse from the model, based on the
original text.

```java
public String generate() {
    String[] lines = this.r.generate(10);
    StringBuffer b = new StringBuffer();
    for (int i=0; i< lines.length; i++) {
        b.append(lines[i]);
        b.append("<br/>\n");
    }
    return b.toString();
}
```

Take a little while to view the code and to get acquainted with it.

To build the application, you are going to use Maven. The `pom.xml` file was generated using [Spring Initializr](https://start.spring.io)
and contains support to use the Spring Native tooling. This is a dependency that you have add to your Spring Boot projects
if you plan to target GraalVM Native Image. If you are using Maven, adding support for Spring Native will insert the following
plugin to your default build configuration.

```xml
<plugin>
    <groupId>org.springframework.experimental</groupId>
    <artifactId>spring-aot-maven-plugin</artifactId>
    <version>${spring-native.version}</version>
    <executions>
        <execution>
            <id>test-generate</id>
            <goals>
                <goal>test-generate</goal>
            </goals>
        </execution>
        <execution>
            <id>generate</id>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Let's build your application. From the root directory of the repository, run the following commands in your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
cd native-image/containerisation/lab
mvn clean package
```

This will generate an "executable" JAR file, one that contains all of the application's dependencies and also a correctly configured `MANIFEST`
file. You can run this JAR file and then "ping" the application's endpoint to see what you get in return.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Run the application in the background
java -jar ./target/jibber-0.0.1-SNAPSHOT-exec.jar &
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you get the some nonsense verse back? OK, so now that you have a built a working application, let's terminate
it and move on to containerising it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Bring the application back to the foreground
fg
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Terminate it with : <ctrl-c>
<ctrl-c>
```

## **STEP 2**: Containerising Your Java Application with Docker

Containerising your Java application as a Docker container is, thankfully, relatively straightforward. You can build
a new Docker image based on one that contains a JDK distribution. So, for this tutorial you will use a container
that already contains a JDK, `ghcr.io/graalvm/native-image:ol8-java17-22` -- this is an Oracle Linux 8 
image with the GraalVM CE JDK 17 installed.

The following is a breakdown of the Dockerfile, which describes how to build the Docker Image. We've added comments to explain the contents.

```dockerfile
# Base Image
FROM ghcr.io/graalvm/native-image:ol8-java17-22

# Pass in the JAR file as an argument to the image build
ARG JAR_FILE                   

# This image will need to expose TCP port 8080, as this is the port on which your app will listen
EXPOSE 8080

# Copy the JAR file from the `target` directory into the root of the image
COPY ${JAR_FILE} app.jar 

# Run Java when starting the container
ENTRYPOINT ["java"]

# Pass in the parameters to the Java command that make it load and run your executable JAR file
CMD ["-jar","app.jar"]
```

The Dockerfile to containerise your Java application can be found in the directory, `native-image/containerisation/lab/00-containerise`.

To build a Docker image containing your application, run the following commands from your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Build a Docker Image
docker build -f ./00-containerise/Dockerfile \
             --build-arg JAR_FILE=./target/jibber-0.0.1-SNAPSHOT-exec.jar \
             -t jibber:jdk.01 .
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# List the newly built image
docker images | head -n2
```

You should see your newly built image listed. Run this image as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker run --rm -d --name "jibber-jdk" -p 8080:8080 jibber:jdk.01
```

Then call the endpoint as you did before - you may need to wait for a few seconds before doing this in order to allow the
application to startup. If you get the following error, `curl: (52) Empty reply from server`, this ill be because the
application is still starting. Wait a few seconds and try again:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you see the nonsense verse? Let's also look at how long it took your application to startup. You can extract this from the logs,
as Spring Boot applications write the time to startup to the logs:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker logs jibber-jdk
```

For example, our application started up in 3.896s -- here is the extract from our logs (Note: the time taken to startup 
will vary from machine to machine):

```shell
2022-03-09 19:48:09.511  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 3.896 seconds (JVM running for 4.583)
```

OK, terminate your container and move on:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker kill jibber-jdk
```

You can also query Docker to get the size of the image. We have provided a script that does this for you. Run the following in your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:jdk.01`
echo $((size_in_bytes/1024/1024))
```

This prints the size of the image in MBs. Ours is `569` MB.

## **STEP 3**: Building a Native Executable

Let's recap what you have so far.

1. You have built a Spring Boot application with a HTTP endpoint, `/jibber`
2. You have successfully containerised it

Now we'll look at how you can create a native executable from your application using GraalVM Native Image. This native executable
is going to have a number of interesting characteristics, namely:

1. It is going to start really fast
2. It will use fewer resources than its corresponding Java application

You can use the native image tooling installed with GraalVM to build a native executable of an
application from the command line, but as you are using Maven already, you are going to use the 
[GraalVM Native Build Tools for Maven](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) which will
conveniently allow you to carry on using maven to build :)

One way of adding support for building a native executable is to use a Maven [profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html), 
which will allow you to decide whether you want to just build the JAR file, or the native executable. 

In the Maven `pom.xml` file provided, we have added a profile that builds a native executable.
Let's take a closer look:

First you need to declare the profile and give it a name.

```xml
<profiles>
    <profile>
        <id>native</id>
        <!-- Rest of profile hidden, to highlight relevant parts -->
    </profile>
</profiles>
```

Next, within the profile, we include the GraalVM Native Image build tools plugin and attach it to the `package` phase in Maven.
This means it will run as a part of the `package` phase. Notice that we can pass configuration arguments to the underlying Native Image
build tool using the `<buildArgs>` section. In individual `buildArg` tags you can pass in parameters in exactly the same way
as you do to the `native-image` tool, so you can use all of the parameters that work with the `native-image` tool:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
            <version>${native-buildtools.version}</version>
            <extensions>true</extensions>
            <executions>
                <execution>
                    <id>build-native</id>
                    <phase>package</phase>
                    <goals>
                        <goal>build</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <imageName>jibber</imageName>
                <buildArgs>
                    <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                </buildArgs>
            </configuration>
        </plugin>
        <!-- Rest of profile hidden, to high-light relevant parts -->
    </plugins>
</build>
```

Now run the Maven build using our profile, as below (note that the profile name is specified with the `-P` flag):

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
mvn package -Pnative
```

This will generate a native executable for your platform in the `target` directory, called `jibber`. Take a 
look at the size of the file:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
ls -lh target/jibber
```

Run this native executable and test it, using the following commands from your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
./target/jibber &
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
curl http://localhost:8080/jibber
```

Now you have a native executable of your application that starts really fast!

Let's terminate the application before you move on.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Bring the application to the foreground
fg
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Terminate it with <ctrl-c>
<ctrl-c>
```

## **STEP 4**: Containerising your Native Executable

So you have a native executable version of your application, and you have seen it working. Let's containerise it.

We have provided a simple Dockerfile for packaging this native executable: it is in the directory
`native-image/containerisation/lab/01-native-image/Dockerfile`. The contents are shown below, along with comments to 
explain each line. This is a two-step docker build, the first part builds the native executable, using GraalVM Native Image, the 
second copies the native executable into a smaller container for use.

>    ### NOTE: Why Are We Using a Two-Step Docker Build?
>
>    When you build a native executable with GraalVM Native Image the executable it builds will be for the platform that 
>    you are running on.
>    
>    So, if you are running on OSX on a Mac, then it will generate a mach64 executable. If you are running on Linux, it will
>    generate an ELF Linux executable (for the architecture of the chip you are running on). If you want to containerise your
>    application in a Docker Image and you are running on OSX, then you can't simply build the executable locally (as that will
>    be an OSX compatible executable) and package it within your Docker Image which expects the executable to be a linux
>    compatible executable. 
> 
>    Therefore you need to build within a Docker container that contains a Linux version of GraalVM, if you are on an 
>    OS other than Linux, in order to create an executable that can be packaged into a Docker container.
>    That is what the multi-stage Docker build we are using here does. The first step in the build process builds a Linux
>    compatible executable and the second one packages that into a Docker Image for deployment.


```dockerfile
FROM ghcr.io/graalvm/graalvm-ce:ol8-java17-22 AS builder

# Install tar and gzip to extract the Maven binaries
RUN microdnf update \
 && microdnf install --nodocs \
    tar \
    gzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

# Install Maven
# Source:
# 1) https://github.com/carlossg/docker-maven/blob/925e49a1d0986070208e3c06a11c41f8f2cada82/openjdk-17/Dockerfile
# 2) https://maven.apache.org/download.cgi
ARG USER_HOME_DIR="/root"
ARG SHA=89ab8ece99292476447ef6a6800d9842bbb60787b9b8a45c103aa61d2f205a971d8c3ddfb8b03e514455b4173602bd015e82958c0b3ddc1728a57126f773c743
ARG MAVEN_DOWNLOAD_URL=https://dlcdn.apache.org/maven/maven-3/3.8.5/binaries/apache-maven-3.8.5-bin.tar.gz

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${MAVEN_DOWNLOAD_URL} \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

# Set the working directory to /home/app
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN mvn clean package -Pnative

# The deployment Image
FROM docker.io/oraclelinux:8-slim

EXPOSE 8080

# Copy the native executable into the containers
COPY --from=builder /build/target/jibber .
ENTRYPOINT ["/jibber"]
```


> ### Building on Linux
> If you are using Linux you don't need to use a multi-stage docker build and your build times will be faster.
> You can just build the native executable locally and package it in our deployment container `01-native-image/Dockerfile.linux`
> as follows:
> ```shell
> mvn clean package -Pnative
> docker build -f ./01-native-image/Dockerfile.linux --build-arg APP_FILE=target/jibber -t jibber:native.01 .
> ```

To build, run the following from your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Build a Docker Image
docker build -f ./01-native-image/Dockerfile \
             -t jibber:native.01 .
# List the newly built image
docker images | head -n2
```

And that is it. You can run this and test it as follows from your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker run --rm -d --name "jibber-native" -p 8080:8080 jibber:native.01
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
curl http://localhost:8080/jibber
```

Again, you should have seen more nonsense verse in the style of the poem Jabberwocky. You can take a look at how long the 
application took to startup by looking at the logs produced by the application as you did earlier. From your shell, 
run the following and look for the startup time:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker logs jibber-native
```
We saw the following which shows that the app started up in 0.074s. A big improvement!

```shell
2022-03-09 19:44:12.642  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.074 seconds (JVM running for 0.081)
```

Let's terminate your container and move onto the next step.
![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker kill jibber-native
```

But before you go to the next step, take a look at the size of the container produced:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:native.01`
echo $((size_in_bytes/1024/1024))
```

The container image size we saw was `171` MB. Quite a lot smaller than our original Java container.

## **STEP 5**: Building a Mostly Static Executable & Packaging it in a Distroless Image

Let's recap, again, what you have done:

1. You have built a Spring Boot application with a HTTP endpoint, `/jibber`
2. You have successfully containerised it
3. You have built a native executable of your application using the Native Image build Tools for Maven
4. You have containerised your native executable

It would be great if we could shrink your container size even further, because smaller containers are quicker to download and start.
With GraalVM Native Image you have the ability to statically link system libraries into the native executable that you
generate. If you build a statically linked native executable, you can package the native executable directly into an empty 
Docker image, also known as a `scratch` container.

Another option is to produce what is known as a mostly-statically linked native executable. With this, you statically link
in all system libraries except for the standard C library, `glibc`. With such a native executable you can use a small container,
such as Google's Distroless which contains the `glibc` library, some standard files, and SSL security certificates. The
standard Distroless container is around 20MB in size.

You will build a mostly-statically linked executable and then package it into a Distroless container.

We have added another Maven profile to build this mostly-statically linked native executable. This profile is named `distroless`.
The only difference between this profile and the one you used before, `native`, is that we pass a parameter, `-H:+StaticExecutableWithDynamicLibC`.
As you might guess this tells `native-image` to build a mostly-statically linked native executable. The following is
te snippet from the `distroless` profile in the `pom.xml` that passes this parameter:

```xml
<buildArgs>
    <!-- Mostly static -->
    <buildArg>-H:+StaticExecutableWithDynamicLibC</buildArg>
    <buildArg>-H:+ReportExceptionStackTraces</buildArg>
</buildArgs>
```

You can build your mostly statically linked native executable as follows - be aware this will only work on Linux. We will
build our native executable in a docker container in the next step, so if you are using OSX, don't worry.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
mvn package -Pdistroless
```

Easy enough. The generated native executable is in the target directory `jibber-distroless`.

And now to package it into a Distroless container. The Dockerfile to do this can be found in the directory 
`native-image/containerisation/lab/02-smaller-containers/Dockerfile`. Again we are going to use a multi-stage build
in order to build a linux executable on all OSes. Take a look at the contents of the Dockerfile, 
which has comments to explain each line:

```dockerfile
FROM ghcr.io/graalvm/graalvm-ce:ol8-java17-22 AS builder

# Install tar and gzip to extract the Maven binaries
RUN microdnf update \
 && microdnf install --nodocs \
    tar \
    gzip \
 && microdnf clean all \
 && rm -rf /var/cache/yum

# Install Maven
# Source:
# 1) https://github.com/carlossg/docker-maven/blob/925e49a1d0986070208e3c06a11c41f8f2cada82/openjdk-17/Dockerfile
# 2) https://maven.apache.org/download.cgi
ARG USER_HOME_DIR="/root"
ARG SHA=89ab8ece99292476447ef6a6800d9842bbb60787b9b8a45c103aa61d2f205a971d8c3ddfb8b03e514455b4173602bd015e82958c0b3ddc1728a57126f773c743
ARG MAVEN_DOWNLOAD_URL=https://dlcdn.apache.org/maven/maven-3/3.8.5/binaries/apache-maven-3.8.5-bin.tar.gz

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${MAVEN_DOWNLOAD_URL} \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

# Set the working directory to /home/app
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN mvn clean package -Pdistroless

# Deployment Containers
# This time we use the distroless image - which is around 20 MB in size. Even smaller versions are available.
FROM gcr.io/distroless/base

ARG APP_FILE
EXPOSE 8080

COPY ${APP_FILE} app
ENTRYPOINT ["/app"]
```

> ### Building on Linux
> If you are using Linux you don't need to use a multi-stage docker build and your build times will be faster.
> You can just build the native executable locally and package it in our deployment container `02-smaller-containers/Dockerfile.linux`
> as follows:
> ```shell
> mvn clean package -Pdistroless
> docker build -f ./02-smaller-containers/Dockerfile.linux --build-arg APP_FILE=target/jibber-distroless -t jibber:distroless.01 .
> ```

To build, run the following from your shell:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# Build a Docker Image
docker build -f ./02-smaller-containers/Dockerfile \
             -t jibber:distroless.01 .
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# List the newly built image
docker images | head -n2
```
And that is it. You can run this and test it as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker run --rm -d --name "jibber-distroless" -p 8080:8080 jibber:distroless.01
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
curl http://localhost:8080/jibber
```
And let's clean up after ourselves by killing the running container.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker kill jibber-distroless
```

Great! It worked. But how small, or large, is your container? Use the script to check the image size:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:distroless.01`
echo $((size_in_bytes/1024/1024))
```

We saw a size of around 82MB. So we have shrunk the container by a lot. A long way down from our starting size, for 
the Java container, of around 600MB.

## Conclusion

We hope you have enjoyed this lab and learnt a few things along the way. We've looked at how you can containerise
a Java application. Then we've seen how to convert that Java application into a native executable, which starts significantly 
faster than the Java application. You then containerised the native executable and have seen how that the size of the 
Docker image, with your native executable in it, is much smaller than the Java Docker Image.

Finally, we looked at how we can build mostly statically linked native executable s with Native Image. These can be
packaged in smaller containers, such as Distroless and these let us shrink the size of the Docker Image even further.

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
