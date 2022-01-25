---
duration: PT0H30M0S
description: Understanding Reflection and GraalVM Native Image
level: Beginner
roles: Application Developer;Technology Manager
lab-id:
products: en/graalvm/enterprise/21
keywords: Java,GraalVM,Cloud Native,Compute
inject-note: true
---
<!-- Our custom styles -->

# Understanding Reflection and GraalVM Native Image

## Introduction

This lab is for developers looking to understand more about how reflection works within
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

Estimated lab time: 30 minutes

### Lab Objectives

In this lab you will perform the following tasks:

- Learn how to handle reflection when using the `native-image` build tool
- Learn about the assisted configuration tooling available

**NOTE:** Whenever you see the laptop icon, this is somewhere you will need to do something. Watch out for these.

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# This is where we you will need to do something
```

## **STEP 1**: The Closed World Assumption

GraalVM native image build uses the closed World assumption, which means that all the bytecode in the application
needs to be known (observed and analysed) at build time.

Before we continue it is worthwhile to go over the build / run model for applications built with GraalVM Native Image.

1. Compile your Java source code into Java byte code classes
2. Using the `native-image` tool, build the Java byte code classes into a native executable
3. Run the native executable

What happens during step 2?

Firstly, the `native-image` tool performs an analysis to see which classes within your application are reachable.
More on this later.

Secondly, found classes, that are known to be safe ([Build-Time Initialization of Native Image Runtime](Build-Time Initialization of Native Image Runtime)),
are initialised. There data is laded into the image heap which then, in turn, get's saved int the native executable. This
is one of the features of the GraalVM Native Image tool that makes for such fast starting applications.

> **NOTE:** : This isn't the same as Object initialisation. Object initialisation happens during the runtime of the native executable.

We said we would return to the topic of reachability. As was mentioned earlier, the analysis determines which classes,
methods and fields need to be included in the native executable. The analysis is static, that is it doesn't  know about
anything about dynamic class loading, reflection etc.

In order to deal with the dynamic features of Java the analysis needs to be told about what classes use reflection, or what classes
are dynamicaly loaded.

> **NOTE:** The analysis can detect certain caases of dynamic class loading and use of reflection.

We can tell the native image build tool about instances of reflection & dynamci class loading thrugh special configuration
files.

So, what can information can we pass to the native image build?

* _Reflection_
* _Resources_
* _JNI_
* _Dynamic Proxies_

For example, classes and methods accessed through the Reflection API need to be configured. There are a few ways
these can be configured, but the most convenient way is through use of the assisted configuration `javaagent`.

## **STEP 2**:  An Example Using Reflection

Imagine you have the following class, `ReflectionExample.java` (a copy of this cana be found in the directory,
`./complete/ReflectionExample.java`):

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

First, let's build it. In your shell, run the following command:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
javac ReflectionExample.java
```

The main method in the class `ReflectionExample` loads a class whose name has been passed in as an argument. The second
method to the class is the method name for that class that should be invoked.

Let's run it and explore the output.

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java ReflectionExample StringReverser reverse "hello"
```

As we expected, the method `foo` on the class `StringReverser` was found, via reflection. The method was invoked and it
reversed our input String of "hello".

OK, but what happens if we try to build a native image out of program? Let's try. In your shell run the following command:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --no-fallback ReflectionExample
```

> **NOTE:** The `--no-fallback` option to `native-image` causes the build to fail if it can not build a stand-alone native executabale.

Now let's run the generated native executable and explore the output:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./reflectionexample StringReverser reverse "hello"

Exception in thread "main" java.lang.ClassNotFoundException: StringReverser
	at com.oracle.svm.core.hub.ClassForNameSupport.forName(ClassForNameSupport.java:60)
	at java.lang.Class.forName(DynamicHub.java:1214)
	at ReflectionExample.main(ReflectionExample.java:21)
```

What happened here? It seems that our native executable was not able to find the class, `StringReverser`. How did this happen?

This happened because the analysis that the `native-image` tool undertook was not able to determine that the class `StringReverser`
was ever used. It then removed the class from the native executable it generated. By removing unwanted classes from the
native executable that is genrated allows the tool to shrink the code that is built and run to only those classes that it
are known to be used. This can casue issues with reflection, but luckily there is a way to deal with this.

## **STEP 3**: Native Image, Assisted Configuration : Enter The Java Agent

The `native-image` build tool is able to load configuration files that define instances of relection, class loading,
JNI, dynamic proxies and resources. If we could add our two classes from our example to this confiuratio () then the
`native-image` tool would know that they needed to be include din the native executable.

Writing a complete reflection configuration file from scratch is certainly possible, but the GraalVM Java runtime provides
a java tracing agent that will generate this for you automatially when you run your application.

Let's try that now. First, we create the directory for the configuration to be saved to:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
mkdir -p META-INF/native-image
```

Then, we run the application with the tracing agent enabled. In our shell run the following:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Note: the tracing agent must come before classpath and jar params on the command ine
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image ReflectionExample StringReverser reverse "hello"
```

### `config-merge-dir`

> We can merge mutiple runs of the java agent by specifying a merge directory when we run the agent. See below for an
> example:
>
> ![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
> ```bash
> JAVA_HOME/bin/java -agentlib:native-image-agent=config-merge-dir=/path/to/config-dir/ ...
> ```

![Tracing Agent Config](../images/tracing-agent-config.png)

Let's explore the created configuration:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
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

You can do this mutiple times and the runs are merged if you specify `native-image-agent=config-merge-dir`, as is shown in the example below:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java -agentlib:native-image-agent=config-merge-dir=META-INF/native-image ReflectionExample StringCapitalizer capitalize "hello"
```

Building the native image now will make use of the provided configuration. Let's build the native executable again:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --no-fallback ReflectionExample
```

And let's see if it works any better:

![](./images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./reflectionexample StringReverser reverse "joker"
```

## Conclusions

This is a very convenient & easy way to configure the reflection and resources that are used by the application when
building native executable with the `native-image tool.

There are a few things you should bear in mind when using the tracing agent:

* Use your test suites. You need to exercise as many paths in your code as you can
* You may need to review & edit your config files

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM EE Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
- [Native Image : Class Initialization](https://www.graalvm.org/reference-manual/native-image/ClassInitialization/)

