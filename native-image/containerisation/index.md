Understanding Containerisation and GraalVM Native Image

## Introduction

This lab is for developers looking to understand more about how to containerise 
[GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/) applications.

GraalVM Native Image technology compiles Java code ahead-of-time into a native executable file. Only the code that is required at run time by the application is included in the executable file.

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

## **STEP 1**: Meet Our Sample Java Application

In this lab you are going to build a simple application with a very minimal REST-based API. You are then going to containerise this application, using Docker.
First, let's take a quick look at our simple application.

We have provided the source code and build scripts for this application in:

```txt
native-image/containerisation/lab/src/
```

The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework and makes use
of the [Spring Native Project](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/) (a Spring incubator
to generate native executables using GraalVM Native Image).

The application has two classes:

* `com.example.demo.DemoApplication` : Our main Spring Boot class, that also defines our HTTP endpoint, `/jibber`
* `com.example.demo.Jabberwocky` : A utility class that implements the logic of the application

So, what does the application do? If you call the endpoint `/jibber`, it will return some nonsense verse generated
in the style of the [Jabberwocky poem](https://en.wikipedia.org/wiki/Jabberwocky), by Lewis Carroll. The program achieves this
by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to model the original poem (this is essentially a statistical model). 
This model generates new text.

In our example application you provide the application with the text of the poem, then generate a model of the text, which the application then uses to 
generate a new text that is simialr to the original text. We are using the [RiTa](https://rednoise.org/rita/) library to do the heavy lifting for us--it supports building and using Markov Chains.

Below are two snippets from the utility class `com.example.demo.Jabberwocky`, that builds the model. The `text` variable
contains the text of the original poem. This snippet shows how we create the model and then populate it with `text`.
This is called from the class constructor
and we define our class to be a [Singleton](https://docs.spring.io/spring-framework/docs/3.0.0.M3/reference/html/ch04s04.html#beans-factory-scopes-singleton)
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

```shell
cd native-image/containerisation/lab
mvn clean package
```

This will generate an "executable" JAR file, one that contains all of its dependencies and also a correctly configured `MANIFEST`
file. You can run this JAR file and "ping" the endpoint to see what you get in return.
<!--I'm not sure this JAR file could be considered to be an "executable"-->

```shell
# Run the application in the background
java -jar ./target/jibber-0.0.1-SNAPSHOT-exec.jar &
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you get the some nonsense verse back? OK, so now that you have a built and working application, let's terminate
it and move on to containerising it.

```shell
# Bring the application back to the foreground
fg
# Terminate it with : <ctrl-c>
<ctrl-c>
```

## **STEP 2**: Containerising Our Java Application with Docker

Containerising your Java application as a Docker container is, thankfully, relatively straightforward. You can build
a new Docker image based on an existing Docker image that contains a JDK distribution. So, for this tutorial you will use a container
that already contains a JDK, `container-registry.oracle.com/java/openjdk:17-oraclelinux8` - this is an Oracle Linux 8 
image with OpenJDK.

The following is a breakdown of the Dockerfile, which describes how to build the Docker Image. Comments have been added
to explain what is happening.

```dockerfile
FROM container-registry.oracle.com/java/openjdk:17-oraclelinux8 # Base Image

ARG JAR_FILE                   # Pass in the JAR file as an argument to the image build

EXPOSE 8080                    # This image will need to expose TCP port 8080, as this is the port on which our app will listen

COPY ${JAR_FILE} app.jar       # Copy the JAR file from the `target` directory into the root of the image 
ENTRYPOINT ["java"]            # Run Java when starting the container
CMD ["-jar","app.jar"]         # Pass in the parameters to the Java command that make it load and run our executable JAR file
```

The Dockerfile to containerise our Java application can be found in the directory, `native-image/containerisation/lab/00-containerise`.

To build a Docker image containing our application, run the following commands from your shell:

```shell
# Build a Docker Image
docker build -f ./00-containerise/Dockerfile \
             --build-arg JAR_FILE=./target/jibber-0.0.1-SNAPSHOT-exec.jar \
             -t jibber:jdk.01 .
# List the newly built image
docker images | head -n2
```

You should see your newly built image listed. Run this image as follows:

```shell
docker run --rm -d --name "jibber-jdk" -p 8080:8080 jibber:jdk.01
```

Then call the endpoint as you did before:

```shell
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you see the nonsense verse? Let's also look at how long it took your application to startup. You can extract this from the logs,
as Spring Boot applications write the time to startup to the logs:

```shell
docker logs jibber-jdk
```

For example, our application started up in 3.896s - extract from the logs below:

```shell
2022-03-09 19:48:09.511  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 3.896 seconds (JVM running for 4.583)
```

OK, let's terminate your container and move on:

```shell
docker kill jibber-jdk
```

You can also query Docker to get the size of the image. We have provided a script that does this for you. Run the following in your shell:

```shell
./scripts/size.sh jibber:jdk.0.1
```

This prints the size of the image in MBs. Ours is `606` MB.

## **STEP 3**: Building a Native Executable

Let's recap what we have so far.

1. We have built a Spring Boot application with a HTTP endpoint, `/jibber`
2. We have successfully containerised it

Now we will look at how we can create a native executable form our application using GraalVM Native Image. This native executable
is going to have a number of interesting characteristics, namely:

1. It is going to start really fast
2. It will use fewer resources than the corresponding Java application

You can use the native image tooling which can be installed with GraalVM in order to build a native executable of an
application from the command line, but as we are using Maven already we are going to use the 
[GraalVM Native Build Tools for Maven](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) which will
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

Let's shut down the application before we move on.

```shell
# Bring the application into the foreground
fg
# Quit it with <ctrl-c>
<ctrl-c>
```

## **STEP 4**: Containerising our Native Executable

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
run the following and look for the startup time:

```shell
docker logs jibber-native
```
We saw the following which shows that the app started up in 0.074s. A big improvement!

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
generate. If you build  statically linked native executable, you can package the native executable directly into an empty 
Docker image, also known as a `scratch` container.

Another option is to produce what is known as a mostly statically linked native executable. With this, we statically link
in all system libraries apart from the standard C library, `glibc`. With such a native executable you can use a small container,
such as Google's Distroless which contains the `glibc` library, some standard files and SSL security certificates. The
standard Distroless container is around 20MB in size.

We will build a mostly statically linked executable and then package it into a Distroless container.

We have added another Maven profile to build this mostly statically linked native executable. This profile is named, `distroless`.
The only difference of this profile to the one we used before, `native`, is that we pass a parameter, `-H:+StaticExecutableWithDynamicLibC`.
As you might guess this tells `native-image` to build one of these mostly statically linked native executables.

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
