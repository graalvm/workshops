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
- Learn how to build a native executable from this application, using GraalVM Native Image, and add it to a Docker Image
- Learn how to shrink your application docker image size with GraalVM Native Image & Distroless containers 

**NOTE:** Whenever you see the laptop icon, this is somewhere you will need to do something. Watch out for these.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# This is where we you will need to do something
```

## **STEP 1**: Meet Our Sample Java Application

In this lab we are going to build a simple application, with a very simple REST based API. We are then going to see how
we can containerize this applciation, using Docker, but first we should take a  look at our sample application.

THe source code and build scripts for this appliction have been provided and can be found in:

```txt
native-image/containerisation/lab/src/
```

The application is built on top of the [Spring Boot](https://spring.io/projects/spring-boot) framework and makes use
of the [Spring Native Project](https://docs.spring.io/spring-native/docs/current/reference/htmlsingle/) (this project is an incubator
for supporting producing neative executables with GraalVM Native IMage for Spring applications).

It is a very simple application that has two classes:

* `com.example.demo.DemoApplication` : Our main Spring Boot class, that also defines our endpoint, `/jibber`
* `com.example.demo.Jabberwocky` : Implements the logic of the application

So, what does the application do? Well if you call the endpoint, `/jibber`, you will get some nonsense verse generated
in the style of the [Jabberwocky poem](https://en.wikipedia.org/wiki/Jabberwocky), by Lewis Carol. The program acheives this
by using a [Markov Chain](https://en.wikipedia.org/wiki/Markov_chain) - this is essentially a statistical model. In our
case we feed in the text of the poem, we generate a model of the text which can then be used to generate new text that
has a similarity to the original text. In the code we are using the [RiTa](https://rednoise.org/rita/) library to do the
heavy lifting for us. Below we can see two snippets form the code.

```java
this.r = new RiMarkov(3);
this.r.addText(text);
```
This snippet shows how we creat ethe model and then populate it with teh text of the originaal poem This is doen in the constructor
and we define our class to be a [Singleton](https://docs.spring.io/spring-framework/docs/3.0.0.M3/reference/html/ch04s04.html#beans-factory-scopes-singleton)
(so only one of them ever gets created).

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

And here we cana see the method that we use for generating new lines of verse, from the model, that are based on the
original text.

Take a little while to look at the code and to get aquianted with it.

To build the application, we are going to use maven. The `pom.xml` file was generated with the [Spring Initializr](https://start.spring.io)
and contains support fo using the Spring Native tooling. This is a dependency that you should have add to your Spring Boot projects
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

So let's build our application. From the root folder of the repository, run the following commands:

```shell
cd native-image/containerisation/lab
mvn clean package
```

This will generate an "executable" jar, one that contains all of it's dependencies and also a correctly configured `MANIFEST`
file. We can try an run this jar and "ping" the endpoint to see what we get back.

```shell
# Run the application in the background
java -jar ./target/jibber-0.0.1-SNAPSHOT-exec.jar &
# Call the endpoint
curl http://localhost:8080/jibber
```

Did you get the some nonsense verse returned back? OK, so now that we have a built and working aplpication, let's kill
it nd move on to containerising it.

```shell
# Bring the application back to the foreground
fg
# Kill it with : <ctrl-c>
```

## **STEP 2**: Containerising Our Java Application with Docker



## **STEP 3**: Building a Native Executable

## **STEP 4**: Containerizing our Native Executable

## **STEP 5**: Building a Mostly Static Executable & Packaging it in a Dostroless Image
