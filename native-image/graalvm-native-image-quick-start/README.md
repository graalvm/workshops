<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg"
alt="GraalVM logo"
width="200px">

# Get Started with GraalVM Native Image

## Introduction
This lab is for developers who want to start building cloud native Java applications using 
[GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/).
<!-- do you want to point to the enterprise version here^^? -->

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

In addition, there are Maven and Gradle plugins for Native Image, so you can easily build,
test, and run Java applications as executable files.

>Note: Oracle Cloud Infrastructure (OCI) provides GraalVM Enterprise at no additional cost.

Estimated lab time: 45 minutes

### Lab Objectives

In this lab you will perform the following tasks:

- Connect to a remote host in Oracle Cloud - you will do the development on an Oracle Cloud server
<!-- I don't think this is required^^ -->
- Build and run a Java application, using GraalVM
- Turn a Java application into a native executable, using GraalVM Native Image
- Build a native executable that works with the dynamic features of Java
- Use the Maven GraalVM plugin to build a native executable using GraalVM Native Image

### Lab Prerequisites

Before starting this lab, you must have installed

* [GraalVM Installation 22 or greater, JDK11 +](https://www.graalvm.org/downloads/) - You can use either the Community or Enterprise Edition 
* The `native-image` tool (see [Native Images](https://www.graalvm.org/22.0/docs/getting-started/#native-images))
* Set your `JAVA_HOME` environment variable to point to your GraalVM installation
* Maven 3.0 or above

>Note: If you see the laptop icon in the lab, this means you need to do something such as enter a command. Keep an 
eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## **STEP 1**: Build and Run the Demo Application

To showcase GraalVM Native Image we are going to use a command line 
Java application that counts the number of files within the current directory and its subdirectories. As a nice extra, it 
also calculates their total size. 

The source code for the application is available in this repository.

### Note on the Demo Application

The application consists of two Java files that can be found in the _src_ directory: 

* _App.java_: a wrapper for the `ListDir` application
* _ListDir.java_: this does all the work--it counts the files and summarizes the output

You can build the application from the command line, or by using Maven profiles. The configuration to build using Maven is provided in 
the _pom.xml_ file. Maven Profiles are a great way to have multiple build configurations within a single _pom.xml_ 
file. For more information about Maven profiles, see [Introduction to Build Profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).

This lab uses two profiles: 

1. `native`: builds a native executable using GraalVM Native Image
2. `java_agent`: builds the Java application with the tracing agent, which tracks all usages of the dynamic code
   in your application and captures this information into configuration files. More on this later.

To use a particular Maven profile, pass it as a parameter to `mvn`. Append the name of the profile to 
the `-P` option.

Now that you have a basic understanding of what the application does, build it and run it to see how it works.

1. Open a terminal window and change to _lab_ directory, this assumes that you are in the root directory of the repository:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   cd native-image/graalvm-native-image-quick-start/lab
   ```

2. Build the application and run it:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw clean package exec:exec
   ```
    The command above performs the following steps:
    1. Clean the project to remove existing generated or compiled artifacts.
    2. Create a JAR file containing the application. The JAR file will be used later by Native Image.
    3. Run the application using the `exec` plugin.

    You should see the following output (the number of files reported by the application may vary):

    ```bash
    Counting directory: .
    Total: 25 files, total size = 3.8 MiB
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    ```

## **STEP 2**: Compile a Java Application into a Native Executable

Next, build a native version of the application using GraalVM Native Image. As a quick reminder, GraalVM 
Native Image compiles your Java application to a native 
executable that does not require a JDK to run, is fast to start and efficient.

1. To begin, check that you have a compiled JAR file in your _target_ directory:

   ```bash
   ls -l ./target
   ```
   
   You should see something similar to:
   
   ```bash
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 archive-tmp
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 classes
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 generated-sources
    -rw-rw-r-- 1 krf krf  496273 Mar  4 11:38 graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar
    -rw-rw-r-- 1 krf krf    7894 Mar  4 11:38 graalvmnidemos-1.0-SNAPSHOT.jar
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 maven-archiver
    drwxrwxr-x 1 krf krf    4096 Mar  4 11:12 maven-status
    ```

    The file you will need is `graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar`.

2. Generate a native image from the command line. You do not need to use the Maven plugin to use GraalVM Native Image, but 
   it can help. The following should be run from the root directory of the project, _lab_:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   native-image \
     -jar ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar \
     --no-fallback \
     -H:Class=oracle.App \
     -H:Name=file-count
   ```

    This will generate an executable file named _file-count_ within the current directory.

3. Run the executable as follows:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./file-count
   ```

4. Time the application running as a native executable and as a regular Java application:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   time ./file-count
   time java -cp ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar oracle.App
   ```

    The native executable runs significantly faster than the corresponding Java 
    application.

Let's dig a little deeper.
What are the various command-line options you passed to the `native-image` command in the step 2. 

* `-jar`: specify the location of the JAR file. You can also specify the classpath using `-cp`.
* `--no-fallback`: do not generate a "fallback" file. A fallback file requires the JVM to run, and you do not need this.
* `-H:Class`: specify which class contains the "entry point" method (the `main()` method).
* `-H:Name`: specify the name of the executable file.

For more information about these options, see [Native Image Build Configuration](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/BuildConfiguration/).
<!-- Are you sure you want to point to the EE docs^^? -->

You can also run the `native-image` tool using the GraalVM Native Image Maven plugin. Look at the provided
_pom.xml_ (Maven configuration) file in the directory. You will find the following snippet that demonstrates how to build 
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
using the `<skip>true</skip>` tag. Note also that you can pass options and parameters to `native-image` through the `<buildArgs/>`
tags.

For more information about the GraalVM Native Image plugin, see [Maven plugin for GraalVM Native Image building](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).

To build the native image using the Maven profile, run:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./mvnw clean package -Pnative
```

The Maven build places the native executable, `file-count`, into the `target` directory.

You can run the native executable as follows:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./target/file-count
```

## **STEP 3**: Using Reflection - Adding a Dependency to Log4J

Say you want to add a library, or some code, to your application that relies upon reflection. A good candidate for 
testing this type of functionality is the [Log4J logging framework](https://logging.apache.org/log4j/2.x/). It is already a dependency in the 
_pom.xml_ file for the application:

```xml
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>
</dependency>
```

To use `log4j`, open the _ListDir.java_ file in an editor and uncomment
a few lines.

1. Open the _ListDir.java_ file using your editor of choice.

2. Remove the comments from the various lines that refer to the logging code. Uncomment the following lines (and remember to save the file when you're done):

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```java
    //import org.apache.log4j.Logger;
    ```
   
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

3. Check if your changes work by rebuilding the application and running it.

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw clean package exec:exec
    ```

   You should see the same kind of output as before with the addition of logging statements.

4. Next, rebuild the native executable using the Maven profile:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw clean package -Pnative
    ```

5. Run the native executable that now contains logging:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./target/file-count
    ```
   
    This generates an error:

    ```
    log4j:ERROR Could not instantiate class [org.apache.log4j.ConsoleAppender].
    java.lang.ClassNotFoundException: org.apache.log4j.ConsoleAppender
        at java.lang.Class.forName(DynamicHub.java:1338)
        at java.lang.Class.forName(DynamicHub.java:1313)
        at org.apache.log4j.helpers.Loader.loadClass(Loader.java:198)
        at org.apache.log4j.helpers.OptionConverter.instantiateByClassName(OptionConverter.java:327)
        at org.apache.log4j.helpers.OptionConverter.instantiateByKey(OptionConverter.java:124)
        at org.apache.log4j.PropertyConfigurator.parseAppender(PropertyConfigurator.java:785)
        at org.apache.log4j.PropertyConfigurator.parseCategory(PropertyConfigurator.java:768)
        at org.apache.log4j.PropertyConfigurator.configureRootCategory(PropertyConfigurator.java:648)
        at org.apache.log4j.PropertyConfigurator.doConfigure(PropertyConfigurator.java:514)
        at org.apache.log4j.PropertyConfigurator.doConfigure(PropertyConfigurator.java:580)
        at org.apache.log4j.helpers.OptionConverter.selectAndConfigure(OptionConverter.java:526)
        at org.apache.log4j.LogManager.<clinit>(LogManager.java:127)
        at org.apache.log4j.Logger.getLogger(Logger.java:117)
        at oracle.ListDir.<clinit>(ListDir.java:75)
        at oracle.App.main(App.java:63)
    log4j:ERROR Could not instantiate appender named "CONSOLE".
    log4j:WARN No appenders could be found for logger (oracle.ListDir).
    log4j:WARN Please initialize the log4j system properly.
    ```
   What just happened here?

### Working with the Dynamic Features of Java
This error is caused by the addition of the Log4J library that relies on reflection. 
The `native-image` tool performs an aggressive static analysis to discover which classes are used within the application: 
for anything not used, the builder assumes that it is not needed. This is called the "closed world" assumption--everything
that needs to be loaded must be known when building a native executable. If it is not findable by the static analysis, then
it is not be included in the native executable.

Reflection is a core feature of Java, so how can you use reflection **and** take advantage of the benefits provided by 
GraalVM Native Image? You need a way to inform the `native-image` tool about any uses of reflection. This is where **configuration files** come in: 
theThe `native-image` tool reads configuration files that specify all 
the classes referenced via reflection.

You can do this by hand, but the GraalVM Java runtime comes with a Java tracing agent that can do this for you. 
It generates JSON files that record all occurrences of reflection, JNI, proxies and access to resources that it can locate
when your application runs.

>Note: It is important to exercise all the code paths in your application when running the tracing agent, in order 
to ensure that all cases of reflection are discovered.

For more information about the tracing agent, see [Assisted Configuration with Tracing Agent](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/).

##  **STEP 4**: Using the Tracing Agent

Use the tracing agent to generate the reflection configuration whilst you run your application.

1. Run the application with the tracing agent, as follows:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/native-image -cp ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar oracle.App
    ```
   
    Inspect the configuration files created by the tracing agent:

   ```bash
   ls -l src/main/resources/META-INF/native-image/
   ```

   You should see something similar to:
   
   ```bash
   total 56
   -rw-r--r--  1 kfoster  staff     4B Dec  2 19:13 jni-config.json
   -rw-r--r--  1 kfoster  staff    86B Nov  9 20:46 native-image.properties
   -rw-r--r--  1 kfoster  staff    65B Dec  2 19:13 predefined-classes-config.json
   -rw-r--r--  1 kfoster  staff     4B Dec  2 19:13 proxy-config.json
   -rw-r--r--  1 kfoster  staff   521B Dec  2 19:13 reflect-config.json
   -rw-r--r--  1 kfoster  staff   101B Dec  2 19:13 resource-config.json
   -rw-r--r--  1 kfoster  staff     4B Dec  2 19:13 serialization-config.json
   ```
   
    >Note: The _pom.xml_ file contains a Maven profile that can do this for you. The following command will run the tracing agent:
   
    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw clean package exec:exec -Pjava_agent
    ```

2. Rebuild the native executable again. This time the `native-image` tool applies configuration files produced by the tracing agent:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw package -Pnative
    ```

3. Finally, run the generated native executable:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./target/file-count
    ```
   
   The native executable works and prints log messages as expected. 
   
This works because the configuration files generated by the tracing agent have recorded the classes that are referenced by reflection. 
The `native-image` tool reads the files and therefore does not exclude the classes from the 
generated executable.

### Note on the position of the `-agentlib` param

Note that the agent parameters **must** come before any `-jar` or `-classpath` parameters. You should also specify a
directory into which to write the files. The recommended location is under `src/main/resources/META-INF/native-image`.
Files placed in this location are picked up automatically by the `native-image` tool.

### Note on Configuring the Native Image Generation

You can also pass parameters to the `native-image` tool using a Java properties files that typically lives 
in _src/main/resources/META-INF/native-image/native-image.properties_. One such file has been included into the _lab_ 
directory to give you an idea of what you can do with it.

## Summary

In this lab, you have tried out several GraalVM Native Image features:

1. How to generate a fast native executable from a Java command line application
2. How to use Maven to build a native executable
3. How to use the tracing agent to automate the process of tracking and registering reflection calls

Write efficient, more secure, and instantly scalable cloud native Java applications with GraalVM Native Image!

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/)
