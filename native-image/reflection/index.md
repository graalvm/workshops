# Understanding Reflection and GraalVM Native Image

## Introduction

This lab takes you step by step through the process of using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/) to build a Java application that uses reflection. 

GraalVM Native Image technology compiles Java code ahead-of-time into a self-contained executable file. Only the code that is required at run time by the application gets added into the executable file.

An executable file produced by Native Image has several important advantages, in that it:

- Uses a fraction of the resources required by the JVM, so is cheaper to run
- Starts in milliseconds
- Deliver peak performance immediately, with no warmup
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
# This is where we you will need to do something
```

## **STEP 1**: The Closed World Assumption

When you use the `native-image` tool (that comes with GraalVM) to create a native executable from a Java application, the tool relies on
what is known as the "closed world" assumption. That is, everything that needs to be included must be known when building a native executable.
If it is not findable by static analysis, it will not be included in the executable file.

Before we continue, let's review the build/run model for applications that are built using GraalVM Native Image.

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
2. The second argument to the `main` method is the name of the method to be called on the dynamically loaded class.
3. The third argument to the `main` method is used as the argument to the method called in (2).

Let's run it and see what it does.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java ReflectionExample StringReverser reverse "hello"
```

As we expected, the method `reverse` on class `StringReverser` was found, via reflection. The method was invoked and it
reversed our input String of "hello". So far, so good.

OK, but what happens if we use the `native-image` tool to build an executable file from our program? Let's try it. In your shell run the following command:

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
By now, I think we probably have an idea why. The "closed world" assumption.

From its static analysis, the `native-image` tool was unable to determine that class `StringReverser`
was ever used and therefore did not include it in the executable it built. Note: By omitting uneccessary classes from the
executable file, the tool reduces the size of the executable file that is built. 
As we have just seen, this can cause issues when used with reflection, but luckily there is a way to deal with this.

## TODO **STEP 3**: Introducing Native Image Reflection Config

We can tell the `native-image` tool about the use of reflection via configuration files. These files are 
written in `JSON` and can be passed ot the `native-image` tool through the use of flags. Here is an example of how we
would do this for our project, if we had created the configuration files, which we haven't done yet:

```bash
# Don't run this yet - as we haven't created the config files yet!
native-image --no-fallback -H:ReflectionConfigurationFiles=config-files/reflect-config.json ReflectionExample
```

So, what other types of configuration information can we pass to the `native-image` build tool? The tooling currently
supports rading files that contain details on:

* _Reflection_
* _Resources_ - resource files that will be required by the application
* _JNI_
* _Dynamic Proxies_
* _Serialisation_

We are only looking at hwo to deal with reflection in this lab, so we will focus on that.

The following is an example o what these files look like (taken from [here](https://www.graalvm.org/22.0/reference-manual/native-image/Reflection/)):

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

From this we can see that classes and methods accessed through the Reflection API need to be configured. we can do this by 
hand, but the most convenient way to generate these configuration files is through use of the assisted configuration 
`javaagent`.

## **STEP 4**: Native Image, Assisted Configuration : Enter The Java Agent

Writing a complete reflection configuration file from scratch is certainly possible, but the GraalVM Java runtime provides 
a java tracing agent, the `javaagent`, that will generate this for you automatically when you run your application. 

Let's try this. First, we will create a directory to save these configuration file into:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
mkdir -p META-INF/native-image
```

Then, we run the application with the tracing agent enabled. In our shell run the following:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
# Note: the tracing agent parameter must come before the classpath and jar params on the command ine
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

You can run this process mutiple times and the runs are merged if you specify `native-image-agent=config-merge-dir`, as i
s shown in the example below:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
java -agentlib:native-image-agent=config-merge-dir=META-INF/native-image ReflectionExample StringCapitalizer capitalize "hello"
```

Building the standalone executable will now make use of the provided configuration. Let's build it:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```bash
native-image --no-fallback ReflectionExample
```

And let's see if it works any better:

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
