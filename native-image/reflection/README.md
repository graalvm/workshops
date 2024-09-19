<picture>
  <source media="(prefers-color-scheme: dark)" srcset="../../images/GraalVM-rgb.png">
  <source media="(prefers-color-scheme: light)" srcset="../../images/GraalVM-rgb.png">
  <img alt="GraalVM logo" src="../../images/GraalVM-rgb.png" width="40%" style="margin-left: -50;">
</picture>

# Using Java Reflection with GraalVM Native Image

## Introduction

This lab takes you step by step through the process of using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/) 
to build an executable from a Java application that relies on reflection. 

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

Estimated lab time: 30 minutes

### Lab Objectives

In this lab you will:

- Use the `native-image` tool to compile a Java application that uses reflection into a native executable file 
- Learn about the assisted configuration tooling provided by GraalVM 

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

## **STEP 1**: The Closed World Assumption

When you use the `native-image` tool (that comes with GraalVM) to create a native executable from a Java application, 
the tool relies on what is known as the "closed world" assumption. That is, everything that needs to be included must be 
known when building a native executable. If it is not findable by static analysis, it will not be included in the 
executable file.

Before you continue, let's review the build/run model for applications that are built using GraalVM Native Image.

1. Compile your Java source code into Java byte code classes
2. Using the `native-image` tool, compile those Java byte code classes into a native executable
3. Run the native executable

But, what really happens during step 2?

Firstly, the `native-image` tool analyses your Java application to determine which classes are reachable. We'll look at 
this in more detail shortly.

Secondly, the tool initializes reachable classes that are safe to be initialized 
(see [Automatic Initialization of Safe Classes](https://www.graalvm.org/22.1/reference-manual/native-image/ClassInitialization/#automatic-initialization-of-safe-classes)).
The class data of the initialized classes is loaded into the heap which is then saved into the executable 
(into the text section of the binary output file). This is one of the features of the GraalVM `native-image` tool that 
enables such fast-starting applications.

>Note: Class initialization isn't the same as Object initialization. Object initialization happens at the runtime of the native executable.

We said we would return to the topic of reachability: the result of `native-image` analysis determines which classes, 
methods, and fields must be included in the native executable. The analysis is static, that is, it doesn't run the Java 
application to determine reachability. The analysis determines many cases of dynamic class loading and uses of 
reflection, but there are some cases that it fails to identify.

To deal with the dynamic features of Java, the analysis must be explicitly provided with details of the classes that use 
reflection, or the classes are dynamically loaded. 

Let's take a look at an example.

## **STEP 2**:  An Example Using Reflection

Imagine you have the following class, `ReflectionExample.java` (a copy of this can be found in the directory, 
_native-image/reflection/lab_):

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

First, build the code. In your terminal, run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
cd native-image/reflection/lab
javac ReflectionExample.java
```

1. The `main` method in class `ReflectionExample` dynamically loads a class whose name has been provided as its first argument.
2. The second argument to the `main` method is the name of the method to be invoked on the dynamically loaded class.
3. The third argument to the `main` method is used as the argument to the method invoked in (2).

Run it and see what it does.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java ReflectionExample StringReverser reverse "hello"
```

As expected, the method `reverse` in class `StringReverser` was found via reflection. The method was invoked and it
reversed our input String of "hello". So far, so good.

What happens if you use the `native-image` tool to build an executable file from our program? Try it: in 
your terminal run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --no-fallback ReflectionExample
```

>Note: The `--no-fallback` option to the `native-image` command causes the build to fail if it can not produce an executable file.

Now, run the executable and see what it does:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./reflectionexample StringReverser reverse "hello"

Exception in thread "main" java.lang.ClassNotFoundException: StringReverser
	at java.lang.Class.forName(DynamicHub.java:1338)
	at java.lang.Class.forName(DynamicHub.java:1313)
	at ReflectionExample.main(ReflectionExample.java:21)
```

What happened here? It seems that your executable was not able to find the class `StringReverser`. How did this happen?
By now, I think we probably have an idea why: the "closed world" assumption.

From its static analysis, the `native-image` tool was unable to determine that class `StringReverser`
was ever used and therefore did not include it in the executable it built. Note: By omitting unnecessary classes,
the tool reduces the size of the resulting file. 
However, as you have just seen, this can cause issues when used with reflection. The next section describes how to resolve this. 

## **STEP 3**: Introducing Native Image Configuration

You use configuration files to inform the `native-image` tool about the use of reflection in an application. The files are 
written in `JSON` and are passed to the `native-image` tool through the use of a command-line option. 

You can pass other types of configuration information to the `native-image` build tool. It currently
supports the following types of configuration:

* _Reflection_
* _Resources_ - resource files that will be required by the application
* _JNI_
* _Dynamic Proxies_
* _Serialization_

However, in this lab we are only looking at how to handle reflection, so we will focus on that.

Here is the contents of an example configuration file (taken from [here](https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/)):

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

You can create this file by 
hand, but a more convenient approach is to generate the configuration using the `javaagent` tool.

## **STEP 4**: Native Image, Assisted Configuration : Enter The Java Agent

Although it's possible to manually create a complete reflection configuration file from scratch, it's not recommended.
Instead, use `javaagent`--a java tracing agent provided by GraalVM. The agent 
generates the configuration for you automatically when you run your application. 

To demonstrate this, first create a directory for the configuration files:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
mkdir -p META-INF/native-image
```

Then, run the application with the tracing agent enabled. In your terminal run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
>Note: the tracing agent option must come before the classpath and jar options on the command line
```bash
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image ReflectionExample StringReverser reverse "hello"
```

![Tracing Agent Config](images/tracing-agent-config.png)

Inspect the configuration file:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
cat META-INF/native-image/reflect-config.json
 ```

```json
[
    {
    "name":"StringReverser",
    "methods":[{"name":"reverse","parameterTypes":["java.lang.String"] }]
    }
]
```

You can run the tracing agent multiple times. The resulting configuration files are merged if you specify 
`native-image-agent=config-merge-dir`, as shown below:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java -agentlib:native-image-agent=config-merge-dir=META-INF/native-image ReflectionExample StringCapitalizer capitalize "hello"
```

Now build the standalone executable to make use of the configuration files.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --no-fallback ReflectionExample
```

And run the executable file to check that it now works as expected:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./reflectionexample StringReverser reverse "hello"
./reflectionexample StringCapitalizer capitalize "hello"
```

It does!

## Summary

The GraalVM Native Image tool relies on what's known as the "closed world" assumption to compile a native executable from 
a Java application. That is, the tool assumes that it can determine which classes, methods, and fields must be included 
in the native executable. If it is not findable by static analysis, it will not be included in the executable file.
To deal with the dynamic features of Java, the analysis must be explicitly provided with details of the classes that use 
reflection, or the classes are dynamically loaded.

The GraalVM platform provides a way to specify to the `native-image` build tool when reflection is used. For
some simple cases, the `native-image` tool can discover these for itself.

The GraalVM platform also provides a way to discover uses of reflection (and other dynamic behaviors) through the Java 
Tracing agent and can automatically generate the configuration files needed by the `native-image` tool. 

There are a few things you should bear in mind when using the tracing agent:

* Use your test suites. You need to exercise as many paths in your code as possible.
* You may need to review and edit your config files by hand.

We hope you have enjoyed this tutorial and have learnt something about how we can deal with reflection when using 
Native Image.

### Learn More
- [Assisted Configuration with Tracing Agent](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/)
- [Class Initialization in Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/ClassInitialization/)
- [GraalVM EE Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/)
- [Reflection Use in Native Images](https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/)
- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
