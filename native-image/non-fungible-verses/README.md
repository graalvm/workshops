# Non Fungible Verses
(Because Web3.0 needs procedurally generated poetry as well as monkey GIFs)

The aims of this workshop are:
1. To introduce you to some of the capabilities of the Oracle Cloud Infrastructure (OCI) always free tier.
2. To show you how using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/) can help you make the most of your free tier allocation.

### OCI "Always Free" Tier

[Oracle Cloud Infrastructure (OCI)](https://www.oracle.com/cloud/) is Oracle's next-generation cloud, designed to run any application, faster and more securely, for less.

OCI has a free tier, which is composed of two parts:
- A free trial of a broad set of OCI services limited by time (30 days) or consumption ($300) limit.
- A set of ["always free"](https://www.oracle.com/cloud/free/#always-free) services that you can use for an unlimited time.

This lab uses two services from the "always free" tier to develop and deploy an application.

You're going to:
- develop and run the application on an ARM-based [Ampere A1 Compute](https://www.oracle.com/cloud/compute/arm/) VM.
- use the OCI [Autonomous JSON Database](https://www.oracle.com/autonomous-database/autonomous-json-database/) service to store the application's data.

The application is built using Spring Boot.
It procedurally generates "poems", and stores them as JSON documents in the autonomous JSON database (AJD).
Each generated poem is publicly accessible via a unique URL.

For a full Web3 experience, you can let people pay for the privilege of recording a transaction on a blockchain that includes the URL of a "poem", thereby "owning" the "digital collectible" non-fungible verse (NFV).
Implementation of a full blown "Web3" solution with blockchain is beyond the scope of this lab.

For further information, see [Web3](https://web3isgoinggreat.com/).

### GraalVM Native Image

[GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) compiles Java code ahead of time into a self-contained native executable.
Only the code that is required by the application at run time is packaged into the executable.
A native executable produced by Native Image has several important advantages, in that it:

- Uses a fraction of the resources required by the JVM, so is cheaper to run
- Starts in milliseconds
- Delivers peak performance immediately, with no warmup
- Can be packaged into a lightweight container image for faster and more efficient deployment
- Presents a reduced attack surface

In addition, there are [Maven and Gradle plugins for Native Image](https://graalvm.github.io/native-build-tools/) so you can easily build, test, and run Java applications as executable files.

> Note: Oracle Cloud Infrastructure (OCI) provides GraalVM Enterprise at no additional cost.

Estimated workshop time: 90 minutes

### Objectives

In this workshop you will:

- Provision an OCI free tier VM
- Clone the poetry application
- Provision an Autonomous JSON Database instance
- Read to and write from the database using with the application
- Add the Spring Boot application to a container image and run it
- Build a native executable from this application, using GraalVM Native Image
- Add the native executable to a container image
- Shrink your application Docker image size with GraalVM Native Image and distroless containers

> Note: If you see the laptop icon in the lab, this means you need to do something such as enter a command.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

### Prerequisites

Before starting this lab, you must have:

* An OCI tenancy - a trial tenancy will do, as the lab can be run using always free services.

### Provisioning OCI services

1. Log into your OCI tenancy.
2. Select or create a compartment to work in (working in the root compartment is a bad idea).

### Compute VM

Create a compute instance
- shape: VM.Standard.A1.Flex
- OCPU: 2
- RAM: 12GB
- Image: vanilla Oracle Linux 9

Once the VM is up and running, SSH into it and carry out the following steps:
* Install [Oracle GraalVM 25](https://docs.oracle.com/en/graalvm/jdk/25/docs/getting-started/oci/compute-instance/).
* Stop and disable the firewall of the VM (normally this isn't recommended).
* In the security list for the VCN Subnet of your VM, add an ingress rule for TCP traffic on port 8080.

## **STEP 1**: Provision an Autonomous JSON Database

1. Create an AJD instance with the default settings. Note your admin password.

2. Once the database is up and running, click *Database Actions*.

3. In the *Database Actions* tab, click *Restful Services and SODA*.
Copy the base URL.
In the _.bashrc_ file of the compute VM, set `$ORDS_BASE_URL` to the base URL.

4. Click *Database Users*.
Create a new user.
In the _.bashrc_ file of the compute VM, set `$ORDS_USER` and `$ORDS_PASSWORD` to the username and password of the user you have created.

5. Log out of *Database Actions*, and then log in as the user you just created.

6. Click *JSON*. Create a collection. In the _.bashrc_ file of the compute VM, set `$JSON_COLLECTION_NAME` to the name of the collection you just created.

    The contents of the _.bashrc_ file of the compute VM should look something like:

    ```bash
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
    export JAVA_HOME="/opt/graalvm-java25"
    export PATH=$JAVA_HOME/bin:$PATH
    export ORDS_BASE_URL="https://nglqexb3yfmaqv5-web3db.adb.us-phoenix-1.oraclecloudapps.com/ords/"
    export ORDS_USER="cyberpoet"
    export ORDS_PASSWORD="September_2022"
    export JSON_COLLECTION_NAME="poetry"
    ```

7. In your SSH terminal(s) run `source .bashrc`.

## **STEP 2**: Review the Sample Application

This workshop describes how to build a simple application with a very minimal REST-based API.
The workshop then shows how to containerize the application, using Docker.
First, take a quick look at the simple application.

The source code and build scripts for this application are in _native-image/containerisation/lab/src_.
The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework.
The application has three classes:

* `com.example.demo.DemoApplication`: The main Spring Boot class that also defines the HTTP endpoint `/jibber`.
* `com.example.demo.Jabberwocky`: A utility class that implements the logic of the application.
* `com.example.demo.DBRestClient`: A class that reads from and writes to the Autonomous JSON Database.

So, what does the application do?

If you call the endpoint `/jibber`, it will return some nonsense verse generated in the style of the [Jabberwocky poem](https://en.wikipedia.org/wiki/Jabberwocky) by Lewis Carroll.
The program achieves this by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) to produce a model of the original poem (this is essentially a statistical model).
This model is then used to generate new text.
- If you call the endpoint `/mint` it will write a new "poem" to the collection in the AJD.
- If you call the endpoint `/read` it will retrieve all the poems from the collection.
- If you call the endpoint `/read/{uuid}` it will retrieve the poem with the specified `uuid` if it exists.

In the example application, you provide it with the text of the poem, then it generates a model of the text which the application then uses to generate a new text that is similar to the original text.
The [RiTa](https://rednoise.org/rita/) library performs the heavy lifting for us--it supports building and using Markov Chains.

Below are two snippets from the utility class `com.example.demo.Jabberwocky` that builds the model.
The `text` variable contains the text of the original poem.
This snippet shows how to create the model and then populate it with `text`.
This is called from the class constructor and the class is defined to be a Singleton (so only one instance of the class ever gets created).

```java
this.r = new RiMarkov(3);
this.r.addText(text);
```

Here you can see the method to generate new lines of verse from the model based on the original text.

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

To build the application, you are going to use Maven.
The _pom.xml_ file contains the [Maven plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

### Action

1. Build the application. From the application directory, run the following commands in your terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    cd native-image/non-fungible-verses/lab
    ./mvnw clean package
    ```
    This will generate a runnable JAR file, one that contains all of the application's dependencies and also a correctly configured `MANIFEST` file.

2. Run this JAR file and then "ping" the application's endpoint to see what you get in return — put the command into the background using `&` so that you get the prompt back.

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -jar ./target/jibber-0.0.1-SNAPSHOT.jar &
    ```

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    curl http://localhost:8080/jibber
    ```
    Did you get the some nonsense verse back?


3. So now that you have a built a working application, bring the application to the foreground by typing `fg`, and then terminate it with CTRL+C.

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    fg
    CTRL+C
    ```

Now move on to running it in a container.

## **STEP 3**: Containerize Your Java Application with Docker

Containerizing a Java application in a container image is straightforward.
You can build a new Docker image based on one that contains a JDK distribution.
For this workshop, you will use a container with the Oracle Linux 9 and the Oracle GraalVM JDK: `container-registry.oracle.com/graalvm/jdk:25`.

The following is a breakdown of the Dockerfile, which describes how to build the Docker image. See the comments to explain the contents.

```dockerfile
# Base Image
FROM container-registry.oracle.com/graalvm/jdk:25

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

The Dockerfile for this step can be found in the directory _native-image/non-fungible-verses/lab/00-containerise_.

### Action

1. Build a Docker image containing your application by running the following command:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker build -f ./00-containerise/Dockerfile \
                --build-arg JAR_FILE=./target/jibber-0.0.1-SNAPSHOT-exec.jar \
                -t jibber:jdk.01 .
    ```
    List the newly built image

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker images | head -n2
    ```

2. Run this image as follows:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker run --rm -d --name "jibber-jdk" -p 8080:8080 jibber:jdk.01
    ```

3. Then call the endpoint as you did before using the `curl` command. (You may need to wait for a few seconds before doing to allow the application to startup.)

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    # Call the endpoint
    curl http://localhost:8080/jibber
    ```
    Did you see the nonsense verse?

4. Now check how long it took your application to start. You can extract this from the logs, as Spring Boot applications write the startup time to the logs:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker logs jibber-jdk
    ```

    The application should start in around 1s. Here is the extract from the logs:
    ```
    2025-05-23T18:14:39.921Z  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.99 seconds (process running for 1.266)
    ```

5. Before going any further in the lab, terminate your container:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker kill jibber-jdk
    ```

6. You can also query Docker to get the size of the container image. Run the following commands in your terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:jdk.01`
    echo $((size_in_bytes/1024/1024))
    ```
    This prints the size of the image in MBs. The size is expected to be around 569MB.

## **STEP 4**: Build a Native Executable

Recap what you have done so far: built a Spring Boot application with a HTTP endpoint, and successfully containerized it.

Now you will look at how you can create a native executable from your application.
This native executable is going to start really fast and use fewer resources than its corresponding Java application.

You can use the `native-image` tool from the GraalVM installation to build a native executable.
But, as you are using Maven already, apply the [Maven Plugin for Native Image](https://graalvm.github.io/native-build-tools/latest/end-to-end-maven-guide.html), which conveniently enables you to carry on using Maven.

One way of building a native executable is to use a [Maven profile](https://maven.apache.org/guides/introduction/introduction-to-profiles.html),
which enables you to decide whether you want to build the JAR file or the native executable.

The Maven _pom.xml_ file contains a profile that builds a native executable:

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
Also, notice that you can pass configuration options to the underlying `native-image` tool using the `<buildArgs>` tag.
In individual `buildArg` tags you can specify parameters in exactly the same way as you do on the command line.

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
                        <goal>compile-no-fork</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <imageName>jibber</imageName>
                <buildArgs>
                    <buildArg>-J-Xmx16G</buildArg>
                </buildArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Action

1. Run the Maven build using the profile, as below (note that the profile name is specified with the `-P` flag):

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw package -Pnative
    ```

    It generates a native executable in the _target_ directory, named _jibber_.
    Take a look at the size of the file:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ls -lh target/jibber
    ```

2. Run this executable and test it, using the following commands from your terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./target/jibber &
    ```
    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    curl http://localhost:8080/jibber
    ```

    A native version of your application starts really fast!

3. Terminate the application before you move on.

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    # Bring the application to the foreground
    fg
    # Terminate it with <ctrl-c>
    <ctrl-c>
    ```

## **STEP 5**: Containerize Your Native Executable

Now, since you have a native executable version of your application, and you have seen it working, containerize it.

A Dockerfile for packaging this native executable is provided for you in  _native-image/non-fungible-verses/lab/01-native-image/Dockerfile_.
The contents are shown below, along with explanatory comments.

This is a two-step build: the first part builds the native executable, using GraalVM Native Image;
and the second step copies the native executable into a deployment container.

>    ### Note: Why to Run a Two-Step Build?
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
FROM container-registry.oracle.com/graalvm/native-image:25 AS builder

# Set the working directory
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN mvn clean package -Pnative

# The deployment container
FROM gcr.io/distroless/java-base-debian12

EXPOSE 8080

# Copy the native executable into the container
COPY --from=builder /build/target/jibber .
ENTRYPOINT ["/jibber"]
```

> ### Note: Building on Linux
> If you are using Linux, you don't need to use a multi-stage Docker build and your build times will be faster.
> You can just build the native executable locally and package it in the deployment container _01-native-image/Dockerfile.linux_ as follows:
> ```bash
> ./mvnw clean package -Pnative
> docker build -f ./01-native-image/Dockerfile.linux --build-arg APP_FILE=target/jibber -t jibber:native.01 .
> ```

### Action

1. To build, run the following from your terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker build -f ./01-native-image/Dockerfile \
                -t jibber:native.01 .
    ```
    List the newly built image:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker images | head -n2
    ```

2. Run this image and test it as follows from your terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker run --rm -d --name "jibber-native" -p 8080:8080 jibber:native.01
    ```
    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    curl http://localhost:8080/jibber
    ```
    Again, you should see more nonsense verse in the style of the poem Jabberwocky.

3. You can see how long the application took to start. From your terminal, run the following command and look for the startup time:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker logs jibber-native
    ```

    You should see a number close to 0.06s. Here is the extract from the logs:
    ```
    2025-05-23T18:24:55.143Z  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.062 seconds (process running for 0.103)
    ```
    That is a big improvement compared to the previous 1s!

4. Terminate your container before moving onto the next step.

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker kill jibber-native
    ```

5. But before you go to the next step, take a look at the size of the container produced:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:native.01`
    echo $((size_in_bytes/1024/1024))
    ```

    The container image size should be around 123MB. Much smaller than the original Java container (690MB)!

## **STEP 6**: Build a Mostly-Static Image and Package It in a Distroless Container

Recap, again, what you have done so far:

- Built a Spring Boot application with a HTTP endpoint, /jibber
- Successfully containerized it
- Built a native executable of your application using the Native Image Build Tools for Maven
- Containerized your native executable

It would be great if you could shrink your container size even further, because smaller containers are quicker to download and start.

With GraalVM Native Image you have the ability to statically link system libraries into the native executable.
If you build a statically linked native executable, you can then package it directly into an empty container image, also known as a "scratch" container.

Another option is to produce what is known as a mostly-statically linked native executable.
With this, you statically link in all system libraries except for the standard C library, `glibc`.
With such a native executable you can use a small container, such as Google's "Distroless" which contains the `glibc` library, some standard files, and SSL security certificates.

In this step, you will build a mostly-statically linked executable and then package it into a Distroless container.

The _pom.xml_ tile contains a Maven profile (named `distroless`) for this build.
The only difference between this profile and the one you used before, `native`, is that it includes the option `--static-nolibc`.
This instructs `native-image` to build a mostly-statically linked native executable.

```xml
<buildArgs>
    <buildArg>--static-nolibc</buildArg>
</buildArgs>
```

The Dockerfile for this step can be found in the directory _native-image/non-fungible-verses/lab/02-smaller-containers/Dockerfile_.
Again you are going to use a multi-stage build.
Take a look at the contents of the Dockerfile, which has comments to explain each line:

```dockerfile
FROM container-registry.oracle.com/graalvm/native-image:25 AS builder

# Set the working directory
WORKDIR /build

# Copy the source code into the image for building
COPY . /build

# Build
RUN mvn clean package -Pnative,distroless

# The deployment container
FROM gcr.io/distroless/base-debian12

ARG APP_FILE
EXPOSE 8080

COPY ${APP_FILE} app
ENTRYPOINT ["/app"]
```

> ### Building on Linux
> If you are using Linux, you do not need to use a multi-stage docker build and your build times will be faster.
> You can just build the native executable locally and package it in our deployment container _02-smaller-containers/Dockerfile.linux_
> as follows:
> ```bash
> ./mvnw package -Pnative,distroless
> docker build -f ./02-smaller-containers/Dockerfile.linux --build-arg APP_FILE=target/jibber-distroless -t jibber:distroless.01 .
> ```

### Action

1. To build, run the following command from your terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker build -f ./02-smaller-containers/Dockerfile \
                -t jibber:distroless.01 .
    ```
    List the newly built image:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker images | head -n2
    ```

2. Run this Docker image and test it:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker run --rm -d --name "jibber-distroless" -p 8080:8080 jibber:distroless.01
    ```
    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    curl http://localhost:8080/jibber
    ```
    As before, you should see more nonsense verse in the style of the poem Jabberwocky.

3. You can check how long the application took to start by inspecting the logs:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker logs jibber-distroless
    ```
    In this run, it should start even faster, around 0.04s:
    ```
    2025-05-23T18:33:03.918Z  INFO 1 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 0.043 seconds (process running for 0.053)
    ```

4. Clean up by terminating the running container:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    docker kill jibber-distroless
    ```
    Great! It worked. But how small, or large, is your container?

5. Use the following commands to check the image size:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    size_in_bytes=`docker inspect -f "{{ .Size }}" jibber:distroless.01`
    echo $((size_in_bytes/1024/1024))
    ```

    The size should be around 110MB. You have shrunk the container by a lot!
    A long way down from the starting container size, the Java container, of 690MB to 110MB.

## Summary

We hope you enjoyed this lab and learnt a few things along the way.
You looked at how you can containerize a Java application.
You saw how to use GraalVM Native Image to compile that Java application into a native executable, which starts significantly faster than its Java counterpart.
Then, you containerized the native executable and saw that the size of the container image, with the native executable in it, is much smaller than the corresponding Java container image. Finally, you looked at how to build mostly-statically linked native executables with Native Image.

### Learn More

- [Tiny Java Containers](https://github.com/graalvm/graalvm-demos/tree/master/native-image/tiny-java-containers)
- [Build a Statically Linked or Mostly-Statically Linked Native Executable](https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/)
- [Building Native Images with Maven: An End-to-End Guide](https://graalvm.github.io/native-build-tools/latest/end-to-end-maven-guide.html)