<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg"
alt="GraalVM logo"
width="200px">

# Non Fungible Verses
(Because Web3.0 needs procedurally generated poetry as well as monkey GIFs)

## Introduction

The aims of this lab are:
1. To introduce you to some of the capabilities of the Oracle Cloud Infrastructure (OCI) always free tier.
2. To show you how using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/) can help you make the most of your free tier allocation.

### OCI "Always Free" Tier

[Oracle Cloud Infrastructure (OCI)](https://www.oracle.com/cloud/) is Oracle's next-generation cloud, designed to run any application, faster and more securely, for less.

OCI has a free tier, which is composed of two parts:
- A free trial of a broad set of OCI services limited by time (30 days) or consumption ($300) limit.
- A set of ["always free"](https://www.oracle.com/cloud/free/#always-free) services that you can use for an unlimited time.

This lab uses two services from the "always free" tier to develop and deploy an application.

You're going to
- develop and run the application on an ARM-based [Ampere A1 Compute](https://www.oracle.com/cloud/compute/arm/ VM.
- use the OCI [Autonomous JSON Database](https://www.oracle.com/autonomous-database/autonomous-json-database/) service to store the application's data.

The application is built using Spring Boot.  It procedually generates "poems", and stores them as JSON documents in the autonomous JSON database (AJD).  We can make each generated poems publicly accessible via a unique URL.

For a full Web3 experince you can let people pay for the privilege of recording a transaction on a blockchain that includes the URL of a "poem", thereby "owning" the "digital collectible" non - fungible verse (NFV).

Implementation of a full blown "Web3" solution with blockchain is beyond the scope of this lab.

Further information on [Web3](https://web3isgoinggreat.com/) may be found here.

### GraalVM Native image

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

>Note: Oracle Cloud Infrastructure (OCI) provides GraalVM Enterprise at no additional cost.

Estimated lab time: 90 minutes

### Lab Objectives

In this lab you will:

- Provision an OCI free tier VM
- Clone the poetry application
- Provision an Autonomous JSON Database instance
- Read to and write from the database using with the application 
- Add the Spring Boot application to a Docker Image and run it
- Build a native executable from this application, using GraalVM Native Image 
- Add the native executable to a Docker Image
- Shrink your application docker image size with GraalVM Native Image and Distroless containers 

>Note: If you see the laptop icon in the lab, this means you need to do something such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## Lab Prerequisites

Before starting this lab, you must have:

* An OCI tenancy - a trial tenancy will do, as the lab can be run using always free services.

## Provisioning OCI services

1. Log into your OCI tenancy
2. Select or create a compartment to work in (working in the root compartment is a bad idea)

### Compute VM

Create a compute instance
- shape: VM.Standard.A1.Flex
- OCPU: 2
- RAM: 12GB
- Image: vanilla Oracle Linux 8

Once the VM is up and running, SSH into it and carry out the following steps:
* Install [GraalVM version 22 or greater, JDK17+](https://www.graalvm.org/downloads/) - You can use either the Community or Enterprise Edition 
* Install the `native-image` tool (see [Native Images](https://www.graalvm.org/22.0/docs/getting-started/#native-images))
* In your `.bashrc` file, set `JAVA_HOME` to point to your GraalVM installation
* stop & disable the firewall of the VM (normally this isn't recommended)
* In the security list for the VCN Subnet of your VM, add an ingress rule for TCP traffic on port 8080.

### Autonomous JSON Database

Create an AJD instance with the default settings.

Note your admin password.

Once the database is up and running, click on "Database Actions".

In the "Database Actions" tab, click on "Restful Services and SODA".  Copy the base URL.  In the `.bashrc` file of the compute VM, set `$ORDS_BASE_URL` to the base URL.

Click on "Database Users".  Create a new user.  In the `.bashrc` file of the compute VM, set `$ORDS_USER` and `$ORDS_PASSWORD` to the username and password of the user you have created.

Log out of "Database Actions", and back in as the user you just created.

Click on JSON.  Create a collection.  In the `.bashrc` file of the compute VM, set `$JSON_COLLECTION_NAME` to the name of the collection you just created.

The `.bashrc` file of the compute VM should look something like:

```
# .bashrc

# Source global definitions
if [ -f /etc/bashrc ]; then
	. /etc/bashrc
fi

# User specific environment
if ! [[ "$PATH" =~ "$HOME/.local/bin:$HOME/bin:" ]]
then
    PATH="$HOME/.local/bin:$HOME/bin:$PATH"
fi
export PATH

# Uncomment the following line if you don't like systemctl's auto-paging feature:
# export SYSTEMD_PAGER=

# User specific aliases and functions
export JAVA_HOME="/opt/graalvm-ee-java17-22.3.0"
export PATH=$JAVA_HOME/bin:$PATH
export ORDS_BASE_URL="https://nglqexb3yfmaqv5-web3db.adb.us-phoenix-1.oraclecloudapps.com/ords/"
export ORDS_USER="cyberpoet"
export ORDS_PASSWORD="September_2022"
export JSON_COLLECTION_NAME="poetry"
```

In your SSH terminal(s) run `source .bashrc`.

## **STEP 1**: Meet Our Sample Java Application

In this lab you are going to build a simple application with a very minimal REST-based API. You are then going to 
containerise the application, using Docker. First, take a quick look at your simple application.

We have provided the source code and build scripts for this application in _native-image/containerisation/lab/src_.

The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework and makes use
of the [Spring Native Project](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/) (a Spring incubator
to generate native executables using GraalVM Native Image).

The application has three classes:

* `com.example.demo.DemoApplication`: The main Spring Boot class that also defines the HTTP endpoint `/jibber`
* `com.example.demo.Jabberwocky`: A utility class that implements the logic of the application
* `com.example.demo.DBRestClient`: A class that reads from and writes to the Autonomous JSON Database.

So, what does the application do?

If you call the endpoint `/jibber`, it will return some nonsense verse generated
in the style of the [Jabberwocky poem](https://en.wikipedia.org/wiki/Jabberwocky) by Lewis Carroll. The program achieves this
by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to produce a model of the original poem (this is essentially a statistical model). 
This model is then used to generate new text.

If you call the endpoint `/mint` it will write a new "poem" to the collection in the AJD.

If you call the endpoint `/read` it will retrieve all the poems from the collection.

If you call the endpoint `/read/{uuid}` it will retrieve the poem with the specified `uuid` if it exists.

In the example application, you provide it with the text of the poem, then generate a model of the text which the application then uses to 
generate a new text that is similar to the original text. We are using the [RiTa](https://rednoise.org/rita/) library to do the heavy lifting for us--it supports building and using Markov Chains.

Below are two snippets from the utility class `com.example.demo.Jabberwocky` that builds the model. The `text` variable
contains the text of the original poem. This snippet shows how we create the model and then populate it with `text`.
This is called from the class constructor
and we define the class to be a [Singleton](https://docs.spring.io/spring-framework/docs/3.0.0.M3/reference/html/ch04s04.html#beans-factory-scopes-singleton)
(so only one instance of the class ever gets created).

```java
this.r = new RiMarkov(3);
this.r.addText(text);
```

Here you can see the method to generate new lines of verse from the model based on the
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

To build the application, you are going to use Maven. The _pom.xml_ file was generated using [Spring Initializr](https://start.spring.io)
and contains support to use the Spring Native tooling. This is a dependency that you must add to your Spring Boot projects
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

Build your application: from the root directory of the repository, run the following commands in your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
cd native-image/containerisation/lab
./mvnw clean package
```

This will generate an "executable" JAR file, one that contains all of the application's dependencies as well as a correctly configured `MANIFEST`
file. You can run this JAR file and then use `curl` to call the application's endpoint to see what you get in return.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Run the application in the background
java -jar ./target/jibber-0.0.1-SNAPSHOT-exec.jar &
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you get the some nonsense verse back? OK, so now that you have a built a working application, terminate
it and move on to containerising it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Bring the application back to the foreground
fg
# Terminate it with : <ctrl-c>
<ctrl-c>
```

## **STEP 2**: Containerising Your Java Application with Docker

Containerising your Java application as a Docker container is straightforward. You can build
a new Docker image based on one that contains a JDK distribution. So, for this tutorial you will use a container
that already contains a JDK, `ghcr.io/graalvm/native-image:ol8-java17-22`--this is an Oracle Linux 8 
image with the GraalVM CE JDK 17 already installed.

The following is a breakdown of the Dockerfile, which describes how to build the Docker Image. The comments explain the contents.

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

The Dockerfile to containerise your Java application can be found in the directory _native-image/containerisation/lab/00-containerise_.

To build a Docker image containing your application, run the following commands from your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Build a Docker Image
docker build -f ./00-containerise/Dockerfile \
             --build-arg JAR_FILE=./target/jibber-0.0.1-SNAPSHOT-exec.jar \
             -t jibber:jdk.01 .
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# List the newly built image
docker images | head -n2
```

You should see your newly built image listed. Run this image as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker run --rm -d --name "jibber-jdk" -p 8080:8080 jibber:jdk.01
```

Then call the endpoint as you did before--you may need to wait for a few seconds before doing to allow the
application to startup. If you get the following error, `curl: (52) Empty reply from server`, this is because the
application is still starting. Wait a few seconds and try again:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you see the nonsense verse? Look at how long it took your application to startup. You can extract this from the logs,
as Spring Boot applications write the time to startup to the logs:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker logs jibber-jdk
```

For example, in our experiments the Java application started up in 3.896s -- here is the extract from our logs (Note: the time taken to startup 
will vary from machine to machine):

```
2022-03-09 19:48:09.511  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 3.896 seconds (JVM running for 4.583)
```

Before going any further in the lab, terminate your container:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker kill jibber-jdk
```

You can also query Docker to get the size of the container image. Run the following commands in your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:jdk.01`
echo $((size_in_bytes/1024/1024))
```

This prints the size of the image in MBs. In our experiments, the size was 569MB.

## **STEP 3**: Building a Native Executable

So far you have:

1. Built a Spring Boot application with a HTTP endpoint, `/jibber`
2. Successfully containerised it

Now you can create a native executable from your application using GraalVM Native Image. The native executable
is going to two interesting characteristics, namely:

1. It is going to start really fast
2. It will use fewer resources than its corresponding Java application

You can use the native image tooling installed with GraalVM to build a native executable of an
application from the command line, but as you are using Maven already, you are going to use the 
[GraalVM Native Build Tools for Maven](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) which will
conveniently allow you to carry on using maven.

One way of adding support for building a native executable is to use a Maven [profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html), 
which will allow you to decide whether you want to build the JAR file or the native executable. 

The Maven _pom.xml_ file contains a profile that builds a native executable.
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

Next, within the profile, the GraalVM Native Image build tools plugin is included and attached to the `package` phase in Maven.
This means it will run as a part of the `package` phase. Notice that you can pass configuration options and parameters to the underlying Native Image
build tool using the `<buildArgs>` section. In individual `buildArg` tags you can pass in parameters in exactly the same way
as you do to the `native-image` tool, so you can use all of the parameters that the `native-image` tool accepts:

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
        <!-- Rest of profile hidden, to highlight relevant parts -->
    </plugins>
</build>
```

Now run the Maven build using the profile, as below (note that the profile name is specified with the `-P` flag):

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./mvnw package -Pnative
```
<!-- Should we tell the user to ignore the warnings here ^^? -->

This will generate a native executable in the _target_ directory, named _jibber_. Take a 
look at the size of the file:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
ls -lh target/jibber
```

Run this native executable and test it, using the following commands from your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./target/jibber &
curl http://localhost:8080/jibber
```

Now you have a native executable of your application that starts really fast!

Terminate the application before you move on.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Bring the application to the foreground
fg
# Terminate it with <ctrl-c>
<ctrl-c>
```

## **STEP 4**: Containerising your Native Executable

So you have a native executable version of your application, and you have seen it working. Now to containerise it.

A simple Dockerfile for packaging this native executable is in the directory
_native-image/containerisation/lab/01-native-image/Dockerfile_. The contents are shown below, along with comments to 
explain the contents. This is a two-step docker build: the first part builds the native executable, using GraalVM Native Image; the 
second step copies the native executable into a smaller container for use.

>    ### Note: Why Are We Using a Two-Step Docker Build?
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
>    Therefore, if you are developing on an OS other than Linux, you need to build within a Docker container that contains a 
>    Linux version of GraalVM to create an executable that can be packaged into a Docker container.
>    That is what the multi-stage Docker build here achieves. The first step in the build process builds a Linux
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


> ### Note: Building on Linux
> If you are using Linux you don't need to use a multi-stage docker build and your build times will be faster.
> You can just build the native executable locally and package it in the deployment container _01-native-image/Dockerfile.linux_
> as follows:
> ```bash
> ./mvnw clean package -Pnative
> docker build -f ./01-native-image/Dockerfile.linux --build-arg APP_FILE=target/jibber -t jibber:native.01 .
> ```

To build, run the following from your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Build a Docker Image
docker build -f ./01-native-image/Dockerfile \
             -t jibber:native.01 .
# List the newly built image
docker images | head -n2
```

And that is it. You can run this and test it as follows from your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker run --rm -d --name "jibber-native" -p 8080:8080 jibber:native.01
curl http://localhost:8080/jibber
```

Again, you should see more nonsense verse in the style of the poem Jabberwocky. You can see how long the 
application took to startup by inspecting the logs produced by the application as you did earlier. From your terminal, 
run the following command and look for the startup time:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker logs jibber-native
```
In our experiments, the native executable started up in 0.074s (compared to 3.896s for the Java application). A big improvement!

```
2022-03-09 19:44:12.642  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.074 seconds (JVM running for 0.081)
```

Terminate your container before moving onto the next step.
![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker kill jibber-native
```

But before you go to the next step, take a look at the size of the container produced:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:native.01`
echo $((size_in_bytes/1024/1024))
```

The container image size we saw in our experiments was 171MB. Quite a lot smaller than the original Java container (589MB).

## **STEP 5**: Building a Mostly Static Executable and Packaging it in a Distroless Image

Let's recap, again, what you have done:

1. You have built a Spring Boot application with a HTTP endpoint, `/jibber`
2. You have successfully containerised it
3. You have built a native executable of your application using the Native Image build Tools for Maven
4. You have containerised your native executable

In this step, you will shrink your container size even further--smaller containers are quicker to download and start.
With GraalVM Native Image you have the ability to statically link system libraries into the native executable.
If you build a statically linked native executable, you can package the native executable directly into an empty 
Docker image, also known as a "scratch" container.

Another option is to produce what is known as a mostly-statically linked native executable. With this, you statically link
in all system libraries except for the standard C library, `glibc`. With such a native executable you can use a small container,
such as Google's Distroless which contains the `glibc` library, some standard files, and SSL security certificates. The
standard Distroless container is around 20MB in size.

You will build a mostly-statically linked executable and then package it into a Distroless container.

The _pom.xml_ tile contains a Maven profile (named `distroless`) to build this mostly-statically linked native executable.
The only difference between this profile and the one you used before, `native`, is that it includes the option `-H:+StaticExecutableWithDynamicLibC`.
This instructs `native-image` to build a mostly-statically linked native executable. The following is
a snippet from the `distroless` profile in the _pom.xml_ file that includes this option:

```xml
<buildArgs>
    <!-- Mostly static -->
    <buildArg>-H:+StaticExecutableWithDynamicLibC</buildArg>
    <buildArg>-H:+ReportExceptionStackTraces</buildArg>
</buildArgs>
```

You can build your mostly-statically linked native executable as follows--be aware this will only work on Linux. We will
build our native executable in a docker container in the next step, so if you are using OSX, don't worry.
<!-- I don't understand this first sentence ^^ -->

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./mvnw package -Pdistroless
```

The generated native executable is in the target directory _jibber-distroless_.

Now to package it into a Distroless container. The Dockerfile to do this can be found in the directory 
_native-image/containerisation/lab/02-smaller-containers/Dockerfile_. Again you are going to use a multi-stage build
in order to build a linux executable on all OSes. Take a look at the contents of the Dockerfile, 
which has comments to explain the contents:

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
> You can just build the native executable locally and package it in our deployment container _02-smaller-containers/Dockerfile.linux_
> as follows:
> ```bash
> ./mvnw clean package -Pdistroless
> docker build -f ./02-smaller-containers/Dockerfile.linux --build-arg APP_FILE=target/jibber-distroless -t jibber:distroless.01 .
> ```

To build, run the following command from your terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Build a Docker Image
docker build -f ./02-smaller-containers/Dockerfile \
             -t jibber:distroless.01 .
```

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# List the newly built image
docker images | head -n2
```
You can run this and test it as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker run --rm -d --name "jibber-distroless" -p 8080:8080 jibber:distroless.01
curl http://localhost:8080/jibber
```

<!-- I added this section, but I don't have the numbers  -->
Again, you should see more nonsense verse in the style of the poem Jabberwocky. You can see how long the 
application took to startup by inspecting the logs produced by the application as you did earlier. From your terminal, 
run the following command and look for the startup time:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
docker logs jibber-native
```
In our experiments, the native executable started up in 0.074s (compared to 3.896s for the Java application). A big improvement!

```
2022-03-09 19:44:12.642  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.667 seconds (JVM running for 0.779)
```

Clean up by terminating the running container.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
docker kill jibber-distroless
```

Great! It worked. But how small, or large, is your container? Use the following commands to check the image size:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:distroless.01`
echo $((size_in_bytes/1024/1024))
```

In our experiments, we saw a size of around 82MB, compared to 171MB for an Oracle Linux 8 
image. So we have shrunk the container by a lot. A long way down from our starting size, for 
the Java container, of almost 600MB.

## Summary

We hope you have enjoyed this lab and learnt a few things along the way. We've looked at how you can containerise
a Java application. Then we've seen how to convert that Java application into a native executable, which starts significantly 
faster than the Java application. You then containerised the native executable and have seen how the size of the 
Docker image, with your native executable in it, is much smaller than the Java Docker Image.

Finally, we looked at how we can build a mostly-statically linked native executable with Native Image. These can be
packaged in smaller containers, such as Distroless and these let us shrink the size of the Docker Image even further.

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
