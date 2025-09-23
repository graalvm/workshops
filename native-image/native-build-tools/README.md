# GraalVM Native Build Tools - for Maven

This workshop takes you step by step through the process of building cloud native Java applications with
[GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/), **using Maven**.
It is aimed at developers with some knowledge of Java.

GraalVM Native Image compiles Java code ahead-of-time into a binary—a self-contained executable.
Only the code that is required at run time by the application gets added to the executable.

An executable produced by Native Image has several advantages, in that it:

- Uses a fraction of the resources required by the JVM, so is cheaper to run
- Starts in milliseconds
- Delivers peak performance immediately, with no warmup
- Can be packaged into a lightweight container image for faster and more efficient deployment
- Presents a reduced attack surface

Many of the leading microservice frameworks support ahead-of-time compilation with GraalVM Native Image, including Micronaut, Spring, Helidon, and Quarkus.

There are [Maven and Gradle plugins for Native Image](https://graalvm.github.io/native-build-tools/latest/) to make building, testing, and running Java applications as native executables easy.

Estimated workshop time: 45 minutes

### Objectives

In workshop lab, you will:

- Use the [Maven plugin for GraalVM Native Image](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html) to build, test, and run a demo application.
- Use the plugin to run unit tests on your native executable.
- Use the plugin to create the reflection configuration for your native executable.

### Prerequisites

Before starting this workshop, you must have installed:

* [GraalVM 25](https://www.graalvm.org/downloads/) - you can use either the Community or Enterprise Edition.

> If you see the laptop icon in the text, this means you need to do something such as enter a command.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## **STEP 1**: Understanding the Native Image Building

In this lab you will run a Java application that uses some dynamic features of Java.

When you use the `native-image` tool to create a native executable from a Java application, it relies on being able to discover, at build time, everything that can be referenced within your application code.
This is what is known as the "closed-world assumption".
Everything that needs to be included in the native executable must be known when it is built.
Anything that is not found by static analysis, or not explicitly specified in the configuration supplied to the `native-image` tool, will not be included in the executable.

The build / run model for GraalVM Native Image is the following:

1. Compile your Java source code into Java bytecode using `javac` or `mvn package`.
2. Use the `native-image` tool to build those Java classes into a native executable.
3. Run the native executable.

What happens during step 2?

The `native-image` tool analyses your Java application to determine which classes are reachable.
But, for classes that `native-image` can't determine as required, but may be required at run time, you need to add configuration.

The demo application for this lab requires configuration, and the following steps illustrate how the _Maven plugin for GraalVM Native Image_ can help generate it for you.

## **STEP 2**: Building, Testing, and Running the Application Using Reflection

Imagine you have the following Java application (a copy of it can be found in the directory, _native-image/native-build-tools/lab_):

```java
package com.example.demo;

import java.lang.reflect.Method;

class StringReverser {
    static String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }
}

class StringCapitalizer {
    static String capitalize(String input) {
        return input.toUpperCase();
    }
}

public class DemoApplication {
    public static void main(String[] args) throws ReflectiveOperationException, IllegalArgumentException {
        DemoApplication demo = new DemoApplication();
        System.out.println(demo.doSomething(args));
    }

    public DemoApplication() {
    }

    public String doSomething(String[] args) throws ReflectiveOperationException, IllegalArgumentException {
        if (args == null || args.length != 3) {
            //
            throw new IllegalArgumentException("Usage : Class Method InputString");
        }
        String className = args[0];
        String methodName = args[1];
        String input = args[2];

        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, String.class);
        String result = (String)method.invoke(null, input);
        return result;
    }
}
```

The above code will reflectively load one of the classes, `StringReverser` or  `StringCapitalizer`, and use their methods to transform a String argument.

```java
Class<?> clazz = Class.forName(className);
Method method = clazz.getDeclaredMethod(methodName, String.class);
String result = (String)method.invoke(null, input);
```

There are also unit tests (that cover the various test cases) in _src/test/java/com/example/demo/DemoApplicationTests.java_.

1. In the terminal, enter the application directory and test the demo by running the unit tests:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    cd native-image/native-build-tools/lab
    ```
    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw test
    ```

    You should see the output message that five unit tests ran successfully.

2.  Now build the application on the JVM:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw package
    ```
    This creates a runnable JAR file, with all dependencies, in the _target_ directory.

3. Run the JAR file, which also contains a file named _META-INF/MANIFEST.MF_ to define the main class, as follows:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -jar ./target/demo-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.demo.StringReverser reverse Java
    ```

    It should produce the following output:
    ```bash
    avaJ
    ```

Now that you understand the application, learn how the Maven plugin for GraalVM Native Image enables ahead-of-time compilation with Maven.

## **STEP 3**: Introducing the Native Build Tools, for Maven

You will use a Maven profile (for more information, see [Maven profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html)) to separate the building of the native executable from the standard building and packaging of your Java application.

Open the project configuration file, _pom.xml_, and review it.
Find the profile with the ID `native`.
The profile is included below:

```xml
<profiles>
    <profile>
        <id>native</id>
        <build>
            <plugins>
                ...
                <plugin>
                    <groupId>org.graalvm.buildtools</groupId>
                    <artifactId>native-maven-plugin</artifactId>
                    <version>${native.maven.plugin.version}</version>
                    <!-- Enables Junit Test Support -->
                    <extensions>true</extensions>
                    <executions>
                        <!-- Binds to the package phase - causes the native executable to be created when you run, mvn -Pnative package -->
                        <execution>
                            <id>build-native</id>
                            <goals>
                                <goal>build</goal>
                            </goals>
                            <phase>package</phase>
                        </execution>
                        <!-- Binds to the test phase - causes the JUnit tests to be run as native code when you run, mvn -Pnative test -->
                        <execution>
                            <id>test-native</id>
                            <goals>
                                <goal>test</goal>
                            </goals>
                            <phase>test</phase>
                        </execution>
                    </executions>
                    <!-- This section is used to configure the native image build -->
                    <configuration>
                        <!-- Tracing agent configuration -->
                        <agent>
                            <options>
                                <accessFilterFiles>
                                    <filterFile>access-filter-file=${basedir}/src/test/resources/access-filter.json</filterFile>
                                </accessFilterFiles>
                            </options>
                        </agent>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

The important things to note here are:

1. The `native-maven-plugin` is contained within a profile with ID, `native`, which means that the plugin won't be run unless you activate the profile.
2. You must enable the extensions to the plugin with `<extensions>true</extensions>` to enable the Tracing agent and JUnit support.
3. You can configure the native image build steps in the `configuration` section.
4. You can configure the Tracing agent in the `agent` section.
5. You must define an `access-filter` within the configuration for use by the Tracing agent.

### Notes on Using the Tracing Agent to Generate Configuration

Because the application uses reflection, you have to tell the `native-image` tool about instances of reflection through a special configuration file.
The configuration is written in JSON.

You can write the configuration by hand, but the most convenient and recommended way is to generate the configuration file using the assisted technology: [Tracing agent](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/).
This Java agent will generate the configuration for you, automatically, when you run your application on the JVM.

You can:
1. **Run the unit tests** and use the Native Image Maven plugin to inject the Tracing agent into the JVM. This is described in the next section.
2. **Run your application** and use the Native Image Maven plugin to inject the Tracing agent into the JVM. For more information, see [the documentation](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html#agent-support-running-application).

> Note: You need to exercise as many paths in your code as you can.

### Copying Generated Tracing Agent Configuration to Your Source Tree

You may have noticed that the _pom.xml_ file includes another plugin in the `native` profile section.
When you run the Tracing agent on your unit tests, the configuration file is generated in a location within the _target_ directory, _target/native/agent-output/test_.
To ensure the `native-image` tool picks it up when you build a native executable, the file needs to be relocated to the location that the tool expects it to be in: _src/main/resources/META-INF/native-image_.

The `maven-resources-plugin` automates this task, so that the configuration file automatically get copied into the source tree when they are available.
The following code is required to achieve this:

```xml
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <version>3.0.2</version>
    <executions>
        <execution>
            <id>copy-agent-config</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <!-- The tracing agent config needs to be placed here to be picked up by the native-image tool -->
                <outputDirectory>src/main/resources/META-INF/native-image</outputDirectory>
                <resources>
                    <resource>
                        <!-- The location that the native build tools will write the tracing agent config out to -->
                        <directory>${basedir}/target/native/agent-output/test</directory>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

An alternative solution would be to tell the `native-image` that it should look into the directory named _/target/native/agent-output/test_ for any configuration.
This can be achieved using the `-H:ConfigurationFileDirectories` option.
At the end of this workshop you will see how to pass extra options to `native-image` using the `native-maven-plugin`.

## **STEP 4**: Running the Tracing Agent

Having seen the details of how the plugin works, now use it to generate the configuration.

1. From the terminal, run the following command. It will run your unit tests while at the same time enabling the Tracing agent and generating the necessary configuration:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw -Pnative -DskipNativeTests=true -DskipNativeBuild=true -Dagent=true test
    ```
    This will run unit tests, and activate the default `native` profile.
    The command includes three additional options:

    * `-DskipNativeTests=true`: The plugin can build a native executable from your unit tests and then run that, in order to check that your unit tests work for the natively compiled code. By setting this option to `true`, these tests are not run. (This option is used again later in the lab.)
    * `-DskipNativeBuild=true`: This option stops the plugin from building a native executable of this demo application. (Likewise, this option is used again later in the lab.)
    * `-Dagent=true`: This causes the Tracing agent to be "injected" into the application as it runs the unit tests.

2. Now take a look at the newly created configuration file _reachability-metadata.json_ in the _target/native/agent-output/test_:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    cat target/native/agent-output/test/reachability-metadata.json
    ```

    ```json
    {
    "reflection": [
    {
        "type": "com.example.demo.DemoApplicationTests",
        "allDeclaredFields": true,
        "methods": [
        {
            "name": "<init>",
            "parameterTypes": []
        },
        {
            "name": "testCapitalise",
            "parameterTypes": []
        },
        {
            "name": "testNoParams",
            "parameterTypes": []
        },
        {
            "name": "testNonExistantClass",
            "parameterTypes": []
        },
        {
            "name": "testNonExistantMethod",
            "parameterTypes": []
        },
        {
            "name": "testReverse",
            "parameterTypes": []
        }
        ]
    },
    {
        "type": "com.example.demo.IDontExist"
    },
    {
        "type": "com.example.demo.StringCapitalizer",
        "methods": [
        {
            "name": "capitalize",
            "parameterTypes": [
            "java.lang.String"
            ]
        }
        ]
    },
    {
        "type": "com.example.demo.StringReverser",
        "methods": [
        {
            "name": "reverse",
            "parameterTypes": [
            "java.lang.String"
            ]
        }
        ]
    },
    {
        "type": "org.apiguardian.api.API"
    },
    {
        "type": "org.junit.internal.AssumptionViolatedException"
    },
    {
        "type": "org.junit.jupiter.api.Test"
    },
    {
        "type": "org.junit.jupiter.engine.JupiterTestEngine"
    },
    {
        "type": "sun.security.provider.NativePRNG",
        "methods": [
        {
            "name": "<init>",
            "parameterTypes": [
            "java.security.SecureRandomParameters"
            ]
        }
        ]
    },
    {
        "type": "sun.security.provider.SHA",
        "methods": [
        {
            "name": "<init>",
            "parameterTypes": []
        }
        ]
    }
    ],
    ...
    ```

    You can see the names of the classes loaded via reflection at the top of the file.

### Notes on Using an Access Filter

To use the Tracing agent to generate the configuration when running the unit tests, create an access filter to ensure that the agent excludes certain classes.
For more information, see [Agent Advanced Usage](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/#agent-advanced-usage).

## **STEP 5**: Running the Unit Tests as Native Code

With the Maven plugin for GraalVM Native Image you can also compile your unit tests into a native executable.
It gives you confidence that your code will run as expected as a native executable.

You can enable this behavior by adding `<extensions>true</extensions>` in the plugin configuration.
In Step 5 you overrode it (the `-DskipNativeTests=true` command-line option disables building a native executable of the tests).

Now try building natively compiled versions of your unit tests and run them.

1. From the terminal, run the following command:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw -Pnative -DskipNativeBuild=true -Dagent=true test
    ```

    This does the following:

    1. Compiles your code, if needed.
    2. Injects the Tracing agent and then runs unit tests on the JVM (**not** native).
    3. Compiles a native executable that will run the unit tests, to which it will pass in the newly created configuration.
    4. Runs the native executable version of your tests.

    You should see the following output:

    ```bash
    Test run finished after 3 ms
    [         2 containers found      ]
    [         0 containers skipped    ]
    [         2 containers started    ]
    [         0 containers aborted    ]
    [         2 containers successful ]
    [         0 containers failed     ]
    [         5 tests found           ]
    [         0 tests skipped         ]
    [         5 tests started         ]
    [         0 tests aborted         ]
    [         5 tests successful      ]
    [         0 tests failed          ]
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    ```

2. If you look within the _target_ directory, you should see the native executable of your unit tests _native-tests_. Run it:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./target/native-tests
    ```

## **STEP 6**: Building the Native Executable

So far you have seen that the `-Dagent=true` command-line option injects the Tracing agent into your unit tests.
You have also seen that you can generate a native executable of the unit tests, which you can run independently without using Maven.
Now it is time to build a native executable of your application itself.

1. This time, run the same command as before, but remove the option that switched off native image builing of the application (not tests), `-DskipNativeBuild=true`:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw -Pnative -Dagent=true package
    ```

    This builds a native executable in the _target_ directory.
    The default name for the generated executable is the name of the `artifactID` defined in the Maven _pom.xml_ file.

2. Now run the executable to confirm that it works:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./target/demo com.example.demo.StringReverser reverse hello
    olleh
    ```

## **STEP 7**: Passing Options to `native-image`

In this last step, you learn how pass options to the `native-image` tool while using the Maven plugin for GraalVM Native Image.

1. Take another look at the Maven _pom.xml_ file. Below is a snippet that is commented out within the plugin configuration:
    ```xml
    <!--
    <imageName>DeMo</imageName>
    <buildArgs>
        <buildArg>-Ob</buildArg>
    </buildArgs>
    -->
    ```

    This enables you to pass extra configuration options to the `native-image` build. Take a look at each of them in turn.

    - Firstly, you can specify the name of the output native executable file, as shown in this snippet:
        ```xml
        <imageName>DeMo</imageName>
        ```

    - If you want to pass additional options to `native image`, use `<buildArgs>`. Any of the `native-image` command-line options can be passed in using this mechanism. For example:
        ```xml
        <buildArgs>
            <buildArg>-Ob</buildArg>
        </buildArgs>
        ```
        The `-Ob` option enables quick build mode. For a full list of the available options available with GraalVM Native Image, run the following command:

        ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
        ```bash
        native-image --help
        ```

2. Now uncomment that section in _pom.xml_ and rebuild the application. First, edit the file and remove the comments, then, from the terminal, run the following command:

    ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./mvnw -Pnative -Dagent=true clean package
    ```

    Take a look inside the _target_ directory.
    You can see that the native executable file has been created with its new name `DeMo`.

## Conclusion

In this lab, you have tried out several GraalVM Native Image features:

1. How to generate a native executable for a Java command line application.
2. How to use Maven to build a native executable.
3. How to use the Tracing agent to automate the process of tracking and registering reflection calls.

Note that there is also the [Gradle plugin for Native Image Building](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html).

### Learn More

- [Native Build Tools](https://graalvm.github.io/native-build-tools/)
- [Reachability Metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)