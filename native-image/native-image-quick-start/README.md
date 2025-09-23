# GraalVM Native Image Quick Start

This workshop takes you step by step through the process of building a cloud native Java application with [GraalVM Native Image](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/).
It is aimed at developers with a knowledge of Java.

GraalVM Native Image compiles Java code ahead of time into a self-contained native executable.
Only the code that is required by the application at run time is packaged into the executable.
A native executable produced by Native Image has several important advantages, in that it:

- Uses a fraction of the resources required by the JVM, so is cheaper to run
- Starts in milliseconds
- Delivers peak performance immediately, with no warmup
- Can be packaged into a lightweight container image for faster and more efficient deployment
- Presents a reduced attack surface

In addition, there are [Maven and Gradle plugins for Native Image](https://graalvm.github.io/native-build-tools/) so you can easily build, test, and run Java applications as executable files.

Estimated workshop time: 45 minutes

### Objectives

In this workshop you will perform the following tasks:

- Build and run a Java application, using GraalVM JDK
- Build a Java application into a native executable, using GraalVM Native Image
- Build a native executable that works with the dynamic features of Java
- Use [Maven plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) to generate a native executable

### Prerequisites

Before starting this workshop, you must have installed:

* [GraalVM 25](https://www.graalvm.org/downloads/) - you can use either the Community or Enterprise Edition.

> If you see the laptop icon in the text, this means you need to do something such as enter a command.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## **STEP 1**: Build and Run the Demo Application

#### Notes on the Demo Application

For the demo, you will use a command-line Java application that counts the number of files in the current directory and its subdirectories.
As a nice extra, the application also calculates the total size of the files.

The source code for the application is available in this repository.

The application consists of two Java files that can be found in the in the _/src/main/java/oracle_ directory:
* `App.java`: a wrapper for the `ListDir` class.
* `ListDir.java`: this does all the work. It counts the files, summarizes the output, and prints it to the console.

The application can be built by hand, or by using Maven profiles.
The Maven build configuration is provided _pom.xml_.
[Maven profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html) are a great way to have different build configurations within a single _pom.xml_ file.

#### Action

Now that you have a basic understanding of what the application does, build it and run it to see how it works.

1. Open a terminal window and change to _lab_ directory, this assumes that you are in the root directory of the repository:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   cd native-image/native-image-quick-start/lab
   ```

2. Go to _pom.xml_ and, at line 12, change the path to the main class from `serialization.App` to `oracle.App`. (You will need the other class later in this workshop.)

   ```xml
   <app.main.class>oracle.App</app.main.class>
   ```

3. Build the application and run it:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw clean package exec:exec
   ```

   The command above does the following:
   1. Creates a runnable JAR file containing the application. This JAR file will be later used by Native Image.
   2. Runs the application by running the `exec` plugin.

    In the output you should see the number of files in the current directory, and the size.

## **STEP 2**: Compile a Java Application into a Native Image

Next, you are going to build a native executable version of the application using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/).

#### Action

1. Generate a native image using the JAR file with all dependencies. You do not need to use the Maven plugin for this run. Run the following from the root directory of the project, `demo`:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   native-image -jar ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar -o file-count
   ```

   This will generate an executable file called `file-count` within the current directory.

2. Run this executable as follows:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./file-count
   ```

3. Now time the application: first by running it from the executable, and then by using the regular `java` command:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   time ./file-count
   ```

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   time java -cp ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar oracle.App
   ```

   The executable, generated by `native-image`, is so much faster than the corresponding Java application!

### Notes on Native Image Building

What do the parameters you passed to the `native-image` command in step 1 specify?

* `-jar` : Specifies the location of the JAR file containing the Java application. (You can also specify the classpath with `-cp`.)
* `-o`: Specifies the name of the output executable file.

The full list of options can be found [here](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/overview/Options/).

You can also run the `native-image` tool using the [Maven plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html).
The project `pom.xml` file contains the following snippet that enables the plugin:

```xml
<plugin>
   <groupId>org.graalvm.buildtools</groupId>
   <artifactId>native-maven-plugin</artifactId>
   <version>${native.maven.plugin.version}</version>
   <extensions>true</extensions>
   <executions>
         <execution>
         <id>build-native</id>
         <goals>
            <goal>compile-no-fork</goal>
         </goals>
         <phase>package</phase>
         </execution>
   </executions>
   <configuration>
         <imageName>${exe.file.name}</imageName>
         <mainClass>${app.main.class}</mainClass>
   </configuration>
</plugin>
```

The Native Image Maven plugin performs the heavy lifting of building the executable file.
Note that you can pass any command line option to `native-image` through the `<buildArgs>` tag.

1. To build the executable file using the Maven profile, run:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw clean package -Pnative
   ```

   The Maven build places the executable, `file-count`, into the `target` directory.

2. You can run it as follows:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./target/file-count
   ```

## **STEP 3**: Use Dynamic Features - Serialization with Jackson

In this step, you will build a native image that that works with the dynamic features of Java.

Say you want to add a library, or some code, to your application that relies upon reflection.
A good candidate is serialization and deserialization.
Java serialization, as an example, uses reflection to extract the non-transient fields of an object, using its privileged status to access otherwise inaccessible fields.

You can find the second version of the same application under _src/main/java/serialization_.
It integrates the [Jackson](https://github.com/FasterXML/jackson) library to serialize the output into JSON.

It is already added as a dependency in the project  `pom.xml` file:
```xml
<dependency>
   <groupId>com.fasterxml.jackson.core</groupId>
   <artifactId>jackson-databind</artifactId>
   <version>2.17.0</version>
</dependency>
```

Unlike the previous, simpler version of the application, this one serializes the `FileCount` object to a file, _file-stats.json_, instead of just printing the results to standard output, using Jackson’s `ObjectMapper.writeValue(File, Object)` method.

#### Action

To build and run the second application version, you need to point Maven to the corresponding main class.

1. Go to _pom.xml_ and, at line 12, change the path to the main class from `oracle.App` to `serialization.App`.

   ```xml
   <app.main.class>serialization.App</app.main.class>
   ```

2. Rebuild and run on a JVM:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw clean package exec:exec
   ```

   You should see the same kind of output, but written to a file, _file-stats.json_, in the root of the project.

3. Next, build a native image again using Maven:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw clean package -Pnative
   ```

4. Run the new executable file:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./target/file-count
   ```

   You are expected to see this exception:
   ```bash
   Exception in thread "main" com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class serialization.FileCount and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS). This appears to be a native image, in which case you may need to configure reflection for the class that is to be serialized
      at com.fasterxml.jackson.databind.SerializerProvider.reportBadDefinition(SerializerProvider.java:1330)
      at com.fasterxml.jackson.databind.DatabindContext.reportBadDefinition(DatabindContext.java:414)
   ```

   What just happened here?

### Notes on Working with Reflection

This exception is caused by the addition of the Jackson serializer because it relies on reflection.
The `native-image` tool performs an aggressive static analysis to see which classes are used within the application.
For any classes not used, the tool will assume that they are not needed.
This is called the "closed-world assumption" - everything that needs to be loaded must be known when building an executable file.
If it is not findable by static analysis, then it will not be included in the executable file.

Reflection is a core feature of Java, so how can you use reflection **and** still take advantage of GraalVM Native Image?
You need a way to let `native-image` know about any uses of reflection.

Luckily, the `native-image` tool is able to read in the configuration file that specify all classes that are referenced through reflection.

You can write the configuration manually, or, **we recommend using the Tracing agent that comes with GraalVM**.
The [Tracing agent](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/metadata/AutomaticMetadataCollection/) collects all instances of reflection, JNI, proxies, and resources access that it can locate while your application is running, and generates the JSON file, _reachability-metadata.json_ in the _resources/META-INF/native-image/_ directory.

**Note**: It is important to exercise all the code paths in your application when running the agent in order to ensure that all cases of reflection are identified.

The complete documentation for providing configuration can be found [here](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/metadata/).

## **STEP 4**: Use the Tracing Agent

Now use the [Tracing agent](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/metadata/AutomaticMetadataCollection/) to generate the configuration while you run your application.

1. Run the application with the agent on the JVM:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/native-image -cp ./target/graalvmnidemos-1.0-SNAPSHOT-jar-with-dependencies.jar serialization.App
    ```

   > Note: The _pom.xml_ file contains a Maven profile that can do this for you.
   The following command will run the Tracing agent:

   ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw clean package exec:exec -Pjava_agent
   ```

   Have a look at the configuration file that the agent created:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   cat src/main/resources/META-INF/native-image/reachability-metadata.json
   ```

   You should see the following contents:

   ```json
   {
   "reflection": [
      {
         "type": "com.fasterxml.jackson.databind.ext.Java7SupportImpl",
         "methods": [
         {
            "name": "<init>",
            "parameterTypes": []
         }
         ]
      },
      {
         "type": "serialization.FileCount",
         "allDeclaredFields": true,
         "methods": [
         {
            "name": "getCount",
            "parameterTypes": []
         },
         {
            "name": "getSize",
            "parameterTypes": []
         }
         ]
      }
   ]
   ```

2. Now re-build the executable file again. This time the configuration produced by the agent will be applied:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./mvnw package -Pnative
   ```

   The `native-image` tool searches for the file _reachability-metadata.json_ in the _META-INF/native-image/_ directory on the class path, and automatically includes it in the build process. When multiple files with the same name are found, all of them are considered.

3. Finally, execute the generated file:

   ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
   ```bash
   ./target/file-count
   ```

   The executable file works as expected!

### Note on the position of the `-agentlib` param

Note that the agent parameters **must** come before any `-jar` or `-classpath` parameters. You should also specify a
directory into which to write the files. The recommended location is under `src/main/resources/META-INF/native-image`.
Files placed in this location are picked up automatically by the `native-image` tool.

## Conclusions

In this workshop, you have tried out several GraalVM Native Image features:

1. How to build a native executable for a Java application.
2. How to use Maven to build a native executable.
3. How to use the Tracing agent to automate the process of tracking and recording reflection.

Write efficient, more secure, and instantly-scalable cloud native Java applications with GraalVM Native Image!

### Learn More

- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Building Native Images with Maven: An End-to-End Guide](https://graalvm.github.io/native-build-tools/latest/end-to-end-maven-guide.html)