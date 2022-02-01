# Understanding Reflection and GraalVM Native Image

## Introduction

This lab takes you step by step through the process of using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/) to build a Java application that relies on reflection. 

GraalVM Native Image technology compiles Java code ahead-of-time into a self-contained executable file. Only the code that is required at run time by the application gets added into the executable file.

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

Estimated lab time: 30 minutes

### Lab Objectives

In this lab you will:

- Use the `native-image` tool to build a Java application that uses reflection into an executable file 
- Learn about the assisted configuration tooling provided by GraalVM 

**NOTE:** If you see the laptop icon in the lab, you need to do something such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```shell
# This is where you will need to do something
```

## **STEP 1**: The Closed World Assumption

When you use the `native-image` tool (that comes with GraalVM) to create a native executable from a Java application, the tool relies on
what is known as the "closed world" assumption. That is, everything that needs to be included must be known when building a native executable.
If it is not findable by static analysis, it will not be included in the executable file.

Before you continue, let's review the build/run model for applications that are built using GraalVM Native Image.

1. Compile your Java source code into Java byte code classes
2. Using the `native-image` tool, build those Java byte code classes into a native executable
3. Run the native executable

But, what really happens during step 2?

Firstly, the `native-image` tool analyses your Java application to determine which classes are reachable.
We'll look at this in more detail shortly.

Secondly, the tool initialises reachable classes that are safe to be initialised 
([Automatic Initialization of Safe Classes](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/ClassInitialization/)).
The class data of the initialised classes is loaded into the image heap which is then saved 
into standalone executable (into the text section). This is one of the features of the GraalVM `native-image` tool that 
can make for such fast starting applications.

<!-- What's an "image heap" or the "text section" ? Do we care? -->

> **NOTE:** : Class initialization isn't the same as Object initialization. Object initialization happens at the runtime of the native executable.

We said we would return to the topic of reachability: the result of `native-image` analysis determines which classes, 
methods, and fields must be included in the native executable. The analysis is static, that is, it doesn't run the Java application to determine reachability. 
The analysis determines many cases of dynamic class loading and uses of reflection (see ), but there are some cases that it fails to identify.

To deal with the dynamic features of Java, the analysis must be explicitly provided with details of the classes that use reflection, or the classes
are dynamicaly loaded. 

Let's take a look at an example.

## **STEP 2**:  An Example Using Reflection

Imagine you have the following class, `ReflectionExample.java` (a copy of this cana be found in the directory, 
`demo/ReflectionExample.java`):

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

First, let's build the code. In your shell, run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
cd demo
javac ReflectionExample.java
```

1. The `main` method in class `ReflectionExample` dynamically loads a class whose name has been provided as its first argument.
2. The second argument to the `main` method is the name of the method to be invoked on the dynamically loaded class.
3. The third argument to the `main` method is used as the argument to the method invoked in (2).

Let's run it and see what it does.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java ReflectionExample StringReverser reverse "hello"
```

As expected, the method `reverse` in class `StringReverser` was found via reflection. The method was invoked and it
reversed our input String of "hello". So far, so good.

OK, but what happens if you use the `native-image` tool to build an executable file from our program? Let's try it. In your shell run the following command:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --no-fallback ReflectionExample
```

> **NOTE:** The `--no-fallback` option to `native-image` causes the build to fail if it can not build an executabale file.

Now let's run the executable file and see what it does:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
./reflectionexample StringReverser reverse "hello"

Exception in thread "main" java.lang.ClassNotFoundException: StringReverser
	at com.oracle.svm.core.hub.ClassForNameSupport.forName(ClassForNameSupport.java:60)
	at java.lang.Class.forName(DynamicHub.java:1214)
	at ReflectionExample.main(ReflectionExample.java:21)
```

What happened here? It seems that our executable was not able to find the class `StringReverser`. How did this happen?
By now, I think we probably have an idea why: the "closed world" assumption.

From its static analysis, the `native-image` tool was unable to determine that class `StringReverser`
was ever used and therefore did not include it in the executable it built. Note: By omitting uneccessary classes,
the tool reduces the size of the resulting file. 
However, as you have just seen, this can cause issues when used with reflection. The next section describes how to avoid these issues.

## TODO **STEP 3**: Introducing Native Image Configuration

You use configuration files to inform the `native-image` tool about the use of reflection in an application. The files are 
written in `JSON` and are passed to the `native-image` tool through the use of a command-line option. Here is an example of how you can
do this for your project (if you had created the configuration files):

```bash
# Don't run this yet - as you haven't created the config files yet!
native-image --no-fallback -H:ReflectionConfigurationFiles=config-files/reflect-config.json ReflectionExample
```

You can pass other types of configuration information to the `native-image` build tool. It currently
supports the following types of configuration:

* _Reflection_
* _Resources_ - resource files that will be required by the application
* _JNI_
* _Dynamic Proxies_
* _Serialisation_

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

From this we can see that we declare the classes and methods accessed through the Reflection API. You can create this file by 
hand, but a more convenient approach is to generate the configuration using the `javaagent` tool.

## **STEP 4**: Native Image, Assisted Configuration : Enter The Java Agent

Although it's possible to manually create a complete reflection configuration file from scratch, it's not recommended.
Instead, use `javaagent`--a java tracing agent provided by GraalVM. The agent 
generates the configuration for you automatically when you run your application. 

Let's try this. First, create a directory for the configuration files:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
mkdir -p META-INF/native-image
```

Then, run the application with the tracing agent enabled. In your shell run the following:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Note: the tracing agent option must come before the classpath and jar options on the command ine
java -agentlib:native-image-agent=config-output-dir=META-INF/native-image ReflectionExample StringReverser reverse "hello"
```

![Tracing Agent Config](images/tracing-agent-config.png)

Let's look at the configuration created:

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

You can run the tracing agent mutiple times. The resulting configuration files are merged if you specify `native-image-agent=config-merge-dir`, 
as shown below:

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
```

It does!

## Conclusions

Building standalone executables with GraalVM Native Image relies on the Closed World assumption, that is we need to know 
in advance, when building, standalone executables about any cases of reflection that can occur in our code.

The GraalVM platform provides a way to specify, to the `native-image` build tool, when refletion is used. Note: For
some simple cases, the `native-image` tool can discover these for itself.

The GraalVM platform also provides a way to discover uses of reflection (and other dynamic behaviours) through the Java Tracing agent and
can automatically generate the configuration files needed by the `native-image` tool. 

There are a few things you should bear in mind when using the tracing agent:

* Use your test suites. You need to exercise as many paths in your code as you can
* You may need to review & edit your config files

We hope you have enjoyed this tutorial and have learnt something about how we cana deal with reflection whn using 
Native Image.

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM EE Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
- [Reflection Use in Native Images](https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/)
- [Class Initialization in Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/ClassInitialization/)
- [Assisted Configuration with Tracing Agent](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/)
