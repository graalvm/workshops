Understanding Reflection and GraalVM Native Image

## Introduction

This lab is for developers looking to understand more about how to contairise 
[GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/) applications.

GraalVM Native Image allows the ahead-of-time compilation of a Java application into a self-contained native executable.
With GraalVM Native Image only the code that is required by the application at run time gets added into the native executable.

These native executables have a number of important advantages, in that they:

- Use a fraction of the resources required by the JVM, so cheaper to run
- Starts in milliseconds
- Deliver peak performance immediately, no warmup
- Can be packaged into lightweight container images for faster and more efficient deployments
- Reduced attack surface (more on this in future labs)

Many of the leading microservice frameworks support ahead-of-time compilation with GraalVM Native Image, including
Micronaut, Spring, Helidon, and Quarkus.

Plus, there are Maven and Gradle plugins for Native Image to make building,
testing, and running Java applications as native executables easy.

> **Note:** Oracle Cloud Infrastructure (OCI) provides GraalVM Enterprise at no additional cost.

Estimated lab time: 90 minutes

### Lab Objectives

In this lab you will perform the following tasks:

- Learn how to add a basic Spring Boot application into a Docker Image and run it
- Learn how to build a native executable from this application, using GraalVM Native Image 
- Learn how to add the native executable to a Docker Image
- Learn how to shrink your application docker image size with GraalVM Native Image & Distroless containers 

**NOTE:** Whenever you see the laptop icon, this is somewhere you will need to do something. Watch out for these.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# This is where we you will need to do something
```

## Prerequisites

For this Lab you will need the following:

* [GraalVM Installation 22 or greater, JDK11 +](https://www.graalvm.org/downloads/) - You can use either the Community or Enterprise Edition 
* The `native-image` tool (see [Native Images](https://www.graalvm.org/22.0/docs/getting-started/#native-images))
* Set your `JAVA_HOME` environment variable to point to your GraalVM installation
* Maven 3.0 or above
* [Docker](https://docs.docker.com/get-docker/) or [Podman](https://podman.io/getting-started/installation)

### Running this on Oracle Luna Labs

intro to Luna (screenshot?)
link to lab

### Running this on OCI Free-Tier

link to markdown in repo thata exlpains how to spin up instance on OCI (free-tier) and grab code and install depednecies



## **STEP 1**: Meet Our Sample Java Application

In this lab we are going to build a simple application, with a very simple REST based API. We are then going to see how
we can containerize this application, using Docker, but first we should take a quick look at our simple application.

THe source code and build scripts for this application have been provided for you and can be found in:

```txt
native-image/containerisation/lab/src/
```

The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework and makes use
of the [Spring Native Project](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/) (this project is a Spring incubator
for supporting generating native executables using GraalVM Native Image).

The application has two classes:

* `com.example.demo.DemoApplication` : Our main Spring Boot class, that also defines our HTTP endpoint, `/jibber`
* `com.example.demo.Jabberwocky` : A utility class that implements the logic of the application

So, what does the application do? Well if you call the endpoint, `/jibber`, you will get some nonsense verse generated
in the style of the [Jabberwocky poem](https://en.wikipedia.org/wiki/Jabberwocky), by Lewis Carol. The program achieves this
by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to model the original poem (this is essentially a statistical model). 
This model can then be used to egenrate new text.

In our example application we feed in the text of the poem, then generate a model of the text, which we then be use to 
generate new text that has a similarity to the original text. We are using a library to do the heay lifting for us. The
[RiTa](https://rednoise.org/rita/) library supports building and using Markov Chains.

Below we have two snippets form the utility class, `com.example.demo.Jabberwocky`, that builds the model. The `text` variable
contains the text of the poem. This snippet shows how we create the model and then populate it with the text of the originaal poem This is doen in the constructor
and we define our class to be a [Singleton](https://docs.spring.io/spring-framework/docs/3.0.0.M3/reference/html/ch04s04.html#beans-factory-scopes-singleton)
(so only one of them ever gets created).

```java
this.r = new RiMarkov(3);
this.r.addText(text);
```

Here we can see the method that we use for generating new lines of verse, from the model, that are based on the
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

Take a little while to look at the code and to get acquainted with it.

To build the application, we are going to use Maven. The `pom.xml` file was generated with the [Spring Initializr](https://start.spring.io)
and contains support for using the Spring Native tooling. This is a dependency that you have add to your Spring Boot projects
if you plan to target GraalVM Native Image. If you are using Maven, adding support for Spring Native will add the following
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

Let's build our application. From the root folder of the repository, run the following commands:

```shell
cd native-image/containerisation/lab
mvn clean package
```

This will generate an "executable" jar, one that contains all of its dependencies and also a correctly configured `MANIFEST`
file. We can try and run this jar and "ping" the endpoint to see what we get back.

```shell
# Run the application in the background
java -jar ./target/jibber-0.0.1-SNAPSHOT-exec.jar &
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you get the some nonsense verse back? OK, so now that we have a built and working application, let's kill
it and move on to containerising it.

```shell
# Bring the application back to the foreground
fg
# Kill it with : <ctrl-c>
<ctrl-c>
```

## **STEP 2**: Containerising Our Java Application with Docker

Containerising our Java application as a Docker container is, thankfully, relatively straight-forward. We can build
a Docker image based on another Docker image that contains a JDK distribution. So for this tutorial we will use a container
thata already contains a JDK, `container-registry.oracle.com/java/openjdk:17-oraclelinux8` - this is an Oracle Linux 8 
image wth OpenJDK.

The following is a breakdown of the Dockerfile, which describes how to build the Docker Image. Comments have been added
to explain what is happening.

```dockerfile
FROM container-registry.oracle.com/java/openjdk:17-oraclelinux8 # Base Image

ARG JAR_FILE                   # Pass in the jar file as an argument to the image build

EXPOSE 8080                    # This image will need to expose TCP port 8080, as this is what our app will listen in on

COPY ${JAR_FILE} app.jar       # Copy the JAR file form the `target` folder into the root of the image 
ENTRYPOINT ["java"]            # Run java when starting the container
CMD ["-jar","app.jar"]         # To the java command pass in the params that make it load and run our executable JAR
```

Our Dockerfile for containerising our Java application can be found in the folder, `native-image/containerisation/lab/00-containerise`.

To build a Docker image containing our application, from the shell we can do as follows:

```shell
# Build a Docker Image
docker build -f ./00-containerise/Dockerfile \
             --build-arg JAR_FILE=./target/jibber-0.0.1-SNAPSHOT-exec.jar \
             -t jibber:jdk.01 .
# List the newly built image
docker images | head -n2
```

You should see your newly built image listed. We can run this image as follows:

```shell
docker run --rm -d --name "jibber-jdk" -p 8080:8080 jibber:jdk.01
```

You can then call the endpoint as we did before:

```shell
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you see the nonsense verse? Let's also look at how long it took our application to startup. We can extract this from the logs,
as Spring Boot applications write the time to startup to the logs:

```shell
docker logs jibber-jdk
```

Our application started up in 3.896s - extract from the logs below:

```shell
2022-03-09 19:48:09.511  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 3.896 seconds (JVM running for 4.583)
```

OK, let's kill our container and move on:

```shell
docker kill jibber-jdk
```

We can also query Docker to get the size of the image. We have provided a script that does this for you. From the
command line, run the following:

```shell
./scripts/size.sh jibber:jdk.0.1
```

This outputs the size of the image in MBs. I got `606` MB.

## **STEP 3**: Building a Native Executable

Let's recap what we have so far.

1. We have built a Spring Boot application with a HTTP endpoint, `/jibber`
2. We have successfully containerised it

Now we will look at how we can create a native executable form our application using GraalVM Native Image. This native executable
is going to have a number of interesting characteristics, namely:

1. It is going to start really fast
2. It will use fewer resources than the corresponding Java application

You can use the native image tooling which can be installed with GraalVM in order ot build a native executable of an
application from the command line, but as we are using Maven already we are going to use the 
[GraalVM Native Build Toosl for Maven](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) which will
conveniently allow us to carry on using maven to build :)

One way of adding support for building a native executable is to use a Maven [profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html), 
which will allow us to decide whether we want to just build the JAR, or the native executable. 

In the Maven `pom.xml` file provided in the lab we have added a profile that allows for building a native executable.
Let's take a closer look:

First we need to declare the profile and give it a name.

```xml
<profiles>
    <profile>
        <id>native</id>
        <!-- Rest of profile hidden, to high-light relevant parts -->
    </profile>
</profiles>
```

Next, within the profile, we include the GraalVM native build tools plugin and attach it to the `package` phase in Maven.
This means it will run as a part of the `package` phase. Notice that we can pass configuration to the underlying Native Image
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

Now run the Maven build using our profile, as bellow (note that the profile name is specified with the `-P` flag):

```shell
mvn package -Pnative
```

This will generate a native executable for your platform in the `target` directory, called, `jibber`. You can take a 
look at the size of the file:

```shell
ls -lh target/jibber
```

And then to run this native executable and to test it, from the command line:

```shell
./target/jibber &
curl http://localhost:8080/jibber
```

Now we have a native executable of our application that starts really fast!

Let's shut down te application before we move on.

```shell
# Bring the application into the foreground
fg
# Quit it with <ctrl-c>
<ctrl-c>
```

## **STEP 4**: Containerizing our Native Executable

So we have a native executable version of our application, and we have seen it working. Let's containerise it.

We have provided a simple Dockerfile for packaging this native executable, that can be found in the directory,
`native-image/containerisation/lab/01-native-image/Dockerfile`. The contents are shown below, along with comments to 
explain what each line does.

```shell
FROM container-registry.oracle.com/os/oraclelinux:8-slim

ARG APP_FILE                 # Pass in the native executable
EXPOSE 8080                  # This image will need to expose TCP port 8080, as this is what our app will listen in on

COPY target/${APP_FILE} app  # Copy the native executable into the root and call it app
ENTRYPOINT ["/app"]          # Just run the native executable :)
```

To build run the following from the command line:

```shell
# Build a Docker Image
docker build -f ./01-native-image/Dockerfile \
             --build-arg APP_FILE=./target/jibber \
             -t jibber:native.01 .
# List the newly built image
docker images | head -n2
```

And that is it. We can run this and test it as follows from the command line:

```shell
docker run --rm -d --name "jibber-native" -p 8080:8080 jibber:native.01
curl http://localhost:8080/jibber
```

Again, you should have seen more nonsense verse in the style of the poem Jabberwocky. We can take a look at how long the 
application took to startup, by looking at the logs produced by the application as we did earlier. From the command line 
run the followung and look for the startup time:

```shell
docker logs jibber-native
```
We saw the following whihc shows that the app started up in 0.074s. A big improvement!

```shell
2022-03-09 19:44:12.642  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.074 seconds (JVM running for 0.081)
```

Let's kill our container and move onto the next step.
```shell
docker kill jibber-native
```

But before we do let's take a look at the size of the container produced:

```shell
./scripts/size.sh jibber:jdk.0.1
```

The container image size we saw, `199` MB. Quite a lot smaller than our original Java container.

## **STEP 5**: Building a Mostly Static Executable & Packaging it in a Distroless Image

Let's recap, again, what we have done:

1. We have built a Spring Boot application with a HTTP endpoint, `/jibber`
2. We have successfully containerised it
3. Built a native executable of our application using the Native Image build Tools for Maven
4. Containerised our native executable

It would be great if we could shrink our container size even further, as smaller containers are quicker to download and start.
With GraalVM Native Image we have the ability to statically link system libraries into the native executable that we
generate. If you build  staticly linked native executable, you can package the native executable directly into an empty 
Docker image, also known as a `scratch` container.

Another option is to produce what is known as a mostly statically linked native executable. With this, we statically link
in all system libraries apart from the standard C library, `glibc`. With such a native executable you can use a small container,
such as Google's Distroless which contains the `glibc` library, some standard files and SSL security certificates. The
standard Distroless container is around 20MB in size.

We will build a mostly statically linked executable and then package it into a Distroless contianer.

We have added another Maven profile to build this mostly statically linked native executable. This profile is named, `distroless`.
The only difference of this profile to the one we used before, `native`, is that we pass a parameter, `-H:+StaticExecutableWithDynamicLibC`.
As you might guess this tells `native-image` to build one of these mostly staticly linked native executables.

We can build our mostly statically linked native executable as follows:

```shell
mvn package -Pdistroless
```

Easy enough. The generated native executable is in the target directory, `jibber-distroless`.

And now to package it into a Distroless container. The Dockerfile to do this can be found in the directory, 
`native-image/containerisation/lab/02-smaller-containers/Dockerfile`. Let's take a look at the contents of the Dockerfile, 
which has comments to explain what each line does:

```text
FROM gcr.io/distroless/base # Our base image, which is Distroless

ARG APP_FILE                # Everything else is the same :)
EXPOSE 8080

COPY ${APP_FILE} app
ENTRYPOINT ["/app"]
```

To build run the following from the command line:

```shell
# Build a Docker Image
docker build -f ./02-smaller-containers/Dockerfile \
             --build-arg APP_FILE=./target/jibber-distroless \
             -t jibber:distroless.01 .
# List the newly built image
docker images | head -n2
```

And that is it. We can run this as follows and test it:

```shell
docker run --rm -d --name "jibber-distroless" -p 8080:8080 jibber:native.01
curl http://localhost:8080/jibber
```

Great! It worked. But how small, or large, is our container? We can use our script to check the image size:

```shell
./scripts/size.sh jibber:distroless.01
```

We saw a size of around 140MB. So we have shrunk the container by 59MB. Still a long way down from our starting size, for 
the Java container, of around 600MB.

## Conclusion

We hope you have enjoyed this lab and learnt a few things along the way. We've looked at how you can containerise
a Java application. Then we've seen how to convert that Java application into a native executable, which starts significantly 
faster than the Java application. We then containerised the native executable and have seen how that the size of the 
Docker image, with our native executable in it, is much smaller than the Java Docker Image.

Finally, we looked at how we can build mostly statically linked native executable s with Native Image. These can be
packaged in smaller containers, such as Distroless and these let us shrink the size of the Docker Image even further.

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
