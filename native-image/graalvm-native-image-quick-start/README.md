<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg"
alt="GraalVM logo"
width="200px">

#  Get Started with GraalVM Native Image

## Introduction
This lab is for developers looking to start building cloud native Java applications with 
[GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/).

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

Estimated lab time: 45 minutes

### Lab Objectives

In this lab you will perform the following tasks:

- Connect to a remote host in Oracle Cloud - you will do the development on an Oracle Cloud server
- Build and run a Java application, using GraalVM
- Turn a Java application into a native executable, using GraalVM Native Image
- Build a native executable that works with the dynamic features of Java
- Use the Maven GraalVM plugin to build native executables with GraalVM Native Image

**NOTE:** Whenever you see the laptop icon, this is somewhere you will need to do something. Watch out for these. 

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# This is where we you will need to do something
```

## **STEP 1**: Test Out Your Development Environment

We will use [GraalVM Enterprise 21](https://docs.oracle.com/en/graalvm/enterprise/21/docs/overview/architecture/#graalvm-enterprise-architecture), 
as the Java enviroment for this lab. GraalVM is a high performance JDK distribution from Oracle built on the trusted and 
secure Oracle Java SE.

You can download GraalVM EE from this link, [Download GraalVM EE](https://www.oracle.com/downloads/graalvm-downloads.html?selected_tab=1), 
and full instalation instructions can be found at, [Installation Instructions](https://docs.oracle.com/en/graalvm/enterprise/21/docs/getting-started/installation-macos/).

You will also need to install GraalVM EE Native Image, 
[Download GraalVM EE Native Image](https://www.oracle.com/downloads/graalvm-downloads.html?selected_tab=1), and instructions 
can be found here, [Native Image Installation Instructions](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/). 

Once you have GraalVM EE installed, with Native Image, you can that everything works by running these commands in the terminal:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java -version
``` 

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --version
```

## **STEP 2**: Build and Run a Demo Applicaton

To showcase GraalVM Native Image we are going to need a demo application. For this lab you will use a command line 
Java application that counts the number of files within the current directory and subdirectories. As a nice extra, it 
also calculates their total size. 

The source code for the application is available in your VM instance.

### Note on the Demo Application

The application consists of two Java files that can be found in the `src` directory: 

* `App.java` : a wrapper for the `ListDir` application
* `ListDir.java` : this does all the work, counting the files and summarising the output

The application can be built by hand, or using Maven profiles. Configuration for building with Maven is in 
the `pom.xml` file. Maven Profiles are a great way to have different build configurations within a single `pom.xml` 
file. You can find out more about Maven Proflies [here](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).

Several profiles will be used in this lab, each of which has a particular purpose: 

1. `native` : This builds a native executable using GraalVM Native Image
2. `java_agent` : This builds the Java application with the tracing agent, which tracks all usages of the dynamic code
   in your application and captures this information into configuration files. More on this later.

You use a particular Maven profile by passing it as a parameter to `mvn`. The name of the profile is appended to 
the `-P` flag. The example below shows how you could call a `native` profile, when building with Maven:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
mvn clean package -Pnative
```

Now that you have a basic understanding of what the application does. You can build it and run it to see how it works.

1. Open a terminal window and change to `lab` folder:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   cd native-image/graalvm-native-image-quick-start/lab
   ```

2. Build the project and run:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   mvn clean package exec:exec
   ```
    The command above does the following:
    1. Cleans the project to get rid of any generated or compiled artifacts.
    2. Creates a runnable JAR file with the application within it. This JAR will be later used by Native Image.
    3. Runs the application by running the `exec` plugin.

    You should see the following included in the generated output (the number of files repoerted by the application may vary):

    ```shell
    Counting directory: .
    Total: 25 files, total size = 3.8 MiB
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    ```

## **STEP 3**: Turn a Java Application into a Native Executable

Next, you are going to build a native version of the application with GraalVM Native Image. As a quick reminder, GraalVM 
Native Image is an ahead-of-time compilation technology that converts your Java application into a self-contained native 
executable that does not require a JDK to run, is fast to start and efficient.

GraalVM Native Image is pre-installed on the virtual machine.

1. To begin, check that you have a compiled uber JAR in your `target` dir:

    ```shell
    ls -l ./target
   ```
   
   This is what we saw:
   
   ```shell
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 archive-tmp
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 classes
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 generated-sources
    -rw-rw-r-- 1 krf krf  496273 Mar  4 11:38 graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar
    -rw-rw-r-- 1 krf krf    7894 Mar  4 11:38 graalvmnidemos-1.0-SNAPSHOT.jar
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 maven-archiver
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 maven-status
    ```

    The file you will need is, `graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar`.

2. Generate a native image from the command line. You do not need to use the Maven plugin to use GraalVM Native Image, but 
   it can help. The following should be run from the root folder of the project, `demo`:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
    native-image -jar ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar --no-fallback -H:Class=oracle.App -H:Name=file-count
    ```

    This will generate an executable file called `file-count` within the current directory.

3. Run this executable as follows:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
    ./file-count
    ```

4. Now try timing the application running as a native executable and using the regular `java` command:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
    time ./file-count
    ```

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
    time java -cp ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar oracle.App
    ```

    The native executable, generated by the `native-image` command, runs significantly faster than the corresponding Java 
    application does.

Let's dig a bit deeper into what you just did.

What do the various parameters you passed to the `native-image` command in the step 2 do? 

* `-jar` : You can specify the location of Java executable Jar. You can also specify the classpath with, `-cp`
* `--no-fallback`: Do not generate a fallback image. A fallback image requires the JVM to run, and you do not need this.
* `-H:Class`: Tell the `native-image` builder which class is the entry point method (the `main` method).
* `-H:Name`: Specify what the output executable file should be called.

The full documentation on these can be found [here](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/BuildConfiguration/).

You can also run the `native-image` tool using the GraalVM Native Image Maven plugin. Look at the provided
`pom.xml` (Maven configuration) file in the project. You will find the following snippet that demonstrates how to build 
native executable using the plugin:

```xml
<!-- Native Image -->
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>${native.maven.plugin.version}</version>
    <extensions>true</extensions>
    <executions>
    <execution>
        <id>build-native</id>
        <goals>
        <goal>build</goal>
        </goals>
        <phase>package</phase>
    </execution>
    </executions>
    <configuration>
        <skip>false</skip>
        <imageName>${exe.file.name}</imageName>
        <mainClass>${app.main.class}</mainClass>
        <buildArgs>
            <buildArg>--no-fallback</buildArg>
            <buildArg>--report-unsupported-elements-at-runtime</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

The Native Image Maven plugin does the heavy lifting of running the native image build. You can disable it 
using the `<skip>true</skip>` tag. Note also that we can pass parameters to `native-image` through the `<buildArgs/>`
tags.

The full documentation on the GraalVM Native Image plugin can be found [here](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).

To build the native image using the Maven profile, run:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
mvn clean package -Pnative
```

The Maven build places the native executable, `file-count`, into the `target` directory.

You can run the native executable as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./target/file-count
```

## **STEP 4**: Using Reflection - Adding a Dependency to Log4J

In this step, you will build a native image that that works with the dynamic parts of Java. 

Say you want to add a library, or some code, to your application that relies upon reflection. A good candidate for 
testing this type of functionality is the Log4J logging framework. It has been already added as a dependency in the 
`pom.xml` file for the project:

```xml
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>
</dependency>
```

To make our application code use `log4j`, you will need to open the `ListDir.java` file in a code editor and uncomment
a few lines.

1. Open the `ListDir.java` file using Vim, or your editor of choice:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    vim src/main/java/oracle/ListDir.java
    ```

2. Type `i` to switch into the insert mode, so that you can start editing the file.

3. Go through and uncomment the various lines that add the imports and the logging code. Uncomment the following lines:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```java
    //final static Logger logger = Logger.getLogger(ListDir.class);
    ```

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```java
    /*
    // Add some logging
    if(logger.isDebugEnabled()){
        logger.debug("Processing : " + dirName);
    }
    */
    ```

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```java
    /*
    // Add some logging
    if(logger.isDebugEnabled()){
        logger.debug("Processing : " + f.getAbsolutePath());
    }
    */
    ```

4. Once you finish uncommenting the lines that add support for `log4j`, you need to save the file and quit Vim. To do 
   this, press the Esc key. Then press `:` (colon), type `wq`, and hit Enter. 

   Now that you have added some logging to the demo application, you can check if the changes work by rebuilding and running.

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    mvn clean package exec:exec
    ```

   You should see the same kind of output as previously with the addition of a lot of logging.

6. Next, build a native executable using the Maven profile:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    mvn clean package -Pnative
    ```

7. Run the native executable you built, that now contains logging:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./target/file-count
    ```
   
    This generates an error:

    ```java
    Exception in thread "main" java.lang.NoClassDefFoundError
            at org.apache.log4j.Category.class$(Category.java:118)
            at org.apache.log4j.Category.<clinit>(Category.java:118)
            at java.lang.Class.ensureInitialized(DynamicHub.java:552)
            at oracle.ListDir.<clinit>(ListDir.java:75)
            at oracle.App.main(App.java:63)
    Caused by: java.lang.ClassNotFoundException: org.apache.log4j.Category
            at java.lang.Class.forName(DynamicHub.java:1433)
            at java.lang.Class.forName(DynamicHub.java:1408)
            ... 5 more
    ```
   What just happened here?

### Working with the Dynamic Features of Java
This run-time exception is caused by the addition of the Log4J library, that depends on reflection. When building 
the `native-image` tool performs an aggressive static analysis to see which classes are used within the application. 
For anything not used, the builder will assume that it is not needed. This is called the "closed world" assumption - 
everything that needs to be loaded must be known when building a native executable. If it is not findable by the static analysis, then
it will not be inclued in the native executable.

Reflection is a core feature of Java, so how can you use reflection **and** take advantage of the speed-ups offered by 
GraalVM Native Image? You need a way to let the `native-image` tool know about any uses of reflection. 

Luckily, the `native-image` tool is able to read in configuration files that specify all 
classes that are referenced through reflection.

You can do this by hand, but, the GraalVM Java runtime comes with a Java tracing agent that can do this for you. 
It generates JSON files that record all instances of reflection, JNI, proxies and resources access that it can locate
whilst your application is running.

**Note** : It is important to exercise all the code paths in your application when running the tracing agent, in order 
to ensure that all cases of reflection are picked up by the agent.

The complete documentation on the tracing agent can be found [here](https://www.graalvm.org/reference-manual/native-image/Agent/).

##  **STEP 5**: Using the Tracing Agent

Now you will use the tracing agent to generate the reflection configuration whilst you run your application.

1. Run the application with the tracing agent:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/native-image -cp ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar oracle.App
    ```
   
    Have a look at the configuration files that the tracing agent created:

   ```shell
   ls -l src/main/resources/META-INF/native-image/
   ```

   We saw:
   
   ```shell
   total 56
   -rw-r--r--  1 kfoster  staff     4B Dec  2 19:13 jni-config.json
   -rw-r--r--  1 kfoster  staff    86B Nov  9 20:46 native-image.properties
   -rw-r--r--  1 kfoster  staff    65B Dec  2 19:13 predefined-classes-config.json
   -rw-r--r--  1 kfoster  staff     4B Dec  2 19:13 proxy-config.json
   -rw-r--r--  1 kfoster  staff   521B Dec  2 19:13 reflect-config.json
   -rw-r--r--  1 kfoster  staff   101B Dec  2 19:13 resource-config.json
   -rw-r--r--  1 kfoster  staff     4B Dec  2 19:13 serialization-config.json
   ```
   
    **Note**: The project contains a Maven profile that can do this for you. Running the following command will run the tracing agent:
   
    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    mvn clean package exec:exec -Pjava_agent
    ```

2. Now re-build the native executable again. This time the configuration files output by the tracing agent will be applied:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    mvn package -Pnative
    ```

3. Finally, execute the generated image:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    time ./target/file-count
    ```
   
   The native executable works and produces log messages to the output, as expected. 
   
This works because the files generated by the tracing agent have recorded the classes that are referenced by reflection. 
The `native-image` tool now knows that they are used within the application and therefore does not exclude them from the 
generated executable.

### Note on the position of the `-agentlib` param

Note that the agent parameters **must** come before any `-jar` or `-classpath` parameters. You should also specify a
directory into which to write the files. The recommended location is under `src/main/resources/META-INF/native-image`.
Files placed in this location are picked up automaticatly by the `native-image` tool.

### Note on Configuring the Native Image Generation

You can also pass parameters to the `native-image` tool using a Java properties files that typically lives 
in `src/main/resources/META-INF/native-image/native-image.properties`. One such file has been included into the demo 
folder in order to give you an idea of what you can do with it.

## Conclusions

In this lab, you have tried out several GraalVM Native Image features:

1. How to generate a fast native executable from a Java command line application
2. How to use Maven to build a native executable
3. How to use the tracing agent to automate the process of tracking and registering reflection calls

Write efficient, more secure, and instantly scalable cloud native Java applications with GraalVM Native Image!

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
