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


## Summary

In this lab, you have tried out several GraalVM Native Image features:

1. How to generate a fast native executable from a Java command line application
2. How to use Maven to build a native executable
3. How to use the tracing agent to automate the process of tracking and registering reflection calls

Write efficient, more secure, and instantly scalable cloud native Java applications with GraalVM Native Image!

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/)
