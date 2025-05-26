# Using Java Reflection with GraalVM Native Image

This workshop guides you step by step through the process of using [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) to build a native executable from a Java application that relies on dynamic features such as reflection.

Native Image is a technology to compile Java code ahead-of-time to a binary—a native executable.
With Native Image only the code that is required by the application at run time gets added to the executable.

These native executables have a number of important advantages, in that they:

- Uses a fraction of the resources required by the JVM, so cheaper to run
- Starts in milliseconds
- Deliver peak performance immediately, no warmup
- Can be packaged into lightweight container images for faster and more efficient deployments
- Reduced attack surface

Many of the leading microservice frameworks support ahead-of-time compilation with GraalVM Native Image, including Micronaut, Spring, Helidon, and Quarkus.

There are [Maven and Gradle plugins for Native Image](https://graalvm.github.io/native-build-tools/latest/) to make building, testing, and running Java applications as native executables easy.

### Objectives

In this workshop you will:

- Learn how to compile a Java application that uses reflection ahead-of-time with the `native-image` tool.
- Learn about the assisted configuration tooling available with GraalVM.

### Prerequisites

Before starting this workshop, you must have installed:

* [GraalVM for JDK 24](https://www.graalvm.org/downloads/) - you can use either the Community or Enterprise Edition.

> If you see the laptop icon in the text, this means you need to do something such as enter a command.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## **STEP 1**: Understanding Native Image Building

Building standalone executables with the `native-image` tool is different from building Java applications.
Native Image makes use of what is known as the "closed-world assumption".

The closed-world assumption means all the bytecode in the application that can be called at
run time must be known (observed and analyzed) at build time, i.e., when the `native-image` tool is building a standalone executable.

The build / run model for GraalVM Native Image is the following:

1. Compile your Java source code into Java bytecode.
2. Using the `native-image` tool, build those Java classes into a native executable.
3. Run the native executable.

What happens during step 2?

Firstly, the `native-image` tool performs an analysis to see which classes within your application are reachable.

Secondly, found classes that are known to be "safe" are initialized.
The class data of the initialized classes is loaded into the image heap which then, in turn, gets saved into the standalone executable (into the text section).

> **NOTE:** : Classes initialization isn't the same as objects initialization. Object initialization happens during the runtime of a native executable.

As mentioned earlier, the analysis determines which classes, methods, and fields need to be included in the standalone executable.
The analysis is static, that is it doesn't run the code.
The analysis can determine some case of dynamic class loading and uses of reflection, but there are cases that it won't be able to pick up.

In order to deal with the dynamic features of Java, `native-image` needs to be told about what classes use reflection, or what classes are dynamically loaded.

Lets take a look at an example.

## **STEP 2**: Running an Example Using Reflection

Imagine you have the following Java application, `ReflectionExample.java` (a copy of this can be found in the directory, _native-image/reflection/lab_):

```java
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

public class ReflectionExample {
    public static void main(String[] args) throws ReflectiveOperationException {
        String className = args[0];
        String methodName = args[1];
        String input = args[2];

        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, String.class);
        Object result = method.invoke(null, input);
        System.out.println(result);
    }
}
```

1. First, compile the application. In your terminal, run the following commands:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    cd native-image/reflection/lab
    ```
    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    javac ReflectionExample.java
    ```

    The main method in class `ReflectionExample` dynamically loads a class whose name has been provided as its first argument.
    The second argument to the `main` method is the name of the method to be invoked on the dynamically loaded class.
    The third argument to the `main` method is used as the argument to the method invoked in (2).

2. Run it and see what it does:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java ReflectionExample StringReverser reverse "hello"
    ```

    As expected, the method `reverse` on the class `StringReverser` was found, via reflection.
    The method was invoked and it reversed the input String of "hello".

    But what happens if you try to build a native image for this application?

3. Run the `native-image` build command:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    native-image ReflectionExample
    ```

4. Now run the generated native executable and see what it does:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./reflectionexample StringReverser reverse "hello"
    ```
    You get a `ClassNotFoundException`:
    ```
    Exception in thread "main" java.lang.ClassNotFoundException: StringReverser
      at org.graalvm.nativeimage.builder/com.oracle.svm.core.hub.ClassForNameSupport.forName(ClassForNameSupport.java:215)
      at org.graalvm.nativeimage.builder/com.oracle.svm.core.hub.ClassForNameSupport.forName(ClassForNameSupport.java:183)
      at java.lang.Class.forName(DynamicHub.java:1214)
      at ReflectionExample.main(ReflectionExample.java:21)
    ```

    What happened here? It seems that the native executable was not able to find the class, `StringReverser`.

During the analysis, `native-image` was not able to determine that the class `StringReverser` was ever used.
Therefore it removed the class from the native executable it generated.
Note: By removing unwanted classes from the standalone executable, the tool shrinks the code by only including classes that are known to be used.
This can cause issues with reflection, but luckily there is a way to deal with this.

## **STEP 3**: Introducing Native Image Reflection Configuration

Reflection is a core feature of Java, so how can you use reflection **and** still take advantage of GraalVM Native Image?
You can tell the `native-image` tool about instances of reflection through a special configuration file, _reachability-metadata.json_.
These file is written in the JSON format and can be passed to the `native-image` tool using command-line options.

The tool supports reflection, resource files, JNI, dynamic proxies, and serialization configuration.
This workshop is only looking at how to deal with reflection.

Here is an example of reflection configuration:
```json
[
  {
    "name" : "java.lang.Class",
    "queryAllDeclaredConstructors" : true,
    "queryAllPublicConstructors" : true,
    "queryAllDeclaredMethods" : true,
    "queryAllPublicMethods" : true,
    "allDeclaredClasses" : true,
    "allPublicClasses" : true
  },
  {
    "name" : "java.lang.String",
    "fields" : [
      { "name" : "value" },
      { "name" : "hash" }
    ],
    "methods" : [
      { "name" : "<init>", "parameterTypes" : [] },
      { "name" : "<init>", "parameterTypes" : ["char[]"] },
      { "name" : "charAt" },
      { "name" : "format", "parameterTypes" : ["java.lang.String", "java.lang.Object[]"] }
    ]
  },
  {
    "name" : "java.lang.String$CaseInsensitiveComparator",
    "queriedMethods" : [
      { "name" : "compare" }
    ]
  }
]
```

From this you can see that classes and methods accessed through the Reflection API need to be configured.
The complete documentation for providing configuration can be found [here](https://www.graalvm.org/latest/reference-manual/native-image/metadata/).

## **STEP 4**: Generating Configuration with the Agent

You can write the configuration file manually, but **the most convenient and recommended way** is to generate the configuration using the assisted technology provided with GraalVM: the Tracing agent.
The [Tracing agent](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/) collects all instances of reflection, JNI, proxies, and resources access that it can locate while your application is running, and generates the JSON file, _reachability-metadata.json_ in the _/META-INF/native-image/_ directory.

1. Run the application with the agent enabled in the same terminal:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -agentlib:native-image-agent=config-output-dir=META-INF/native-image ReflectionExample StringReverser reverse "hello"
    ```

2. Have a look at the configuration created:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    cat META-INF/native-image/reachability-metadata.json
    ```

    ```json
    {
      "reflection": [
        {
          "type": "StringReverser",
          "methods": [
            {
              "name": "reverse",
              "parameterTypes": [
                "java.lang.String"
              ]
            }
          ]
        }
      ]
    ```

3. You can run this process multiple times. The new entries will be merged if you specify `native-image-agent=config-merge-dir`, as is shown in the example below:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    java -agentlib:native-image-agent=config-merge-dir=META-INF/native-image ReflectionExample StringCapitalizer capitalize "hello"
    ```

    > Note: It is important to exercise all the code paths in your application when running the agent in order to ensure that all cases of reflection are identified.

4. The `native-image` should now make use of the provided configuration. Run the build again:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    native-image ReflectionExample
    ```

5. Let's see if it works any better:

    ![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
    ```bash
    ./reflectionexample StringReverser reverse "hello"
    ./reflectionexample StringCapitalizer capitalize "hello"
    ```

    It does!

Learn more how to [generate configuration with the Tracing agent from the documentation](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/).

## Summary

Building standalone executables with GraalVM Native Image relies on the closed-world assumption.
Native Image needs to know in advance about any cases of reflection that can occur in the code.

GraalVM provides a way to discover uses of reflection and other dynamic Java features through the Tracing agent and can automatically generate the configuration needed by the `native-image` tool.

There are a few things you should bear in mind when using the Tracing agent:

* Run your test suites. You need to exercise as many paths in your code as you can.
* You may need to review and edit your configuration files.

### Learn More

- [Reachability Metadata for Dynamic Features of Java](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)
- [Collect Metadata with the Tracing Agent](https://www.graalvm.org/latest/reference-manual/native-image/metadata/AutomaticMetadataCollection/)