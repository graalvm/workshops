<picture>
  <source media="(prefers-color-scheme: dark)" srcset="images/GraalVM-rgbrev.png">
  <source media="(prefers-color-scheme: light)" srcset="images/GraalVM-rgb.png">
  <img alt="GraalVM logo" src="images/GraalVM-rgb.png" width="40%" style="margin-left: -50;">
</picture>

# ![Lab Flask](./images/lab-flask.png) GraalVM Workshops and Tutorials

This repository contains several workshops and tutorials that will guide you through getting started with 
and using GraalVM. It also provides workshops that deep-dive into particular topics, such as how reflection 
and Class loading works within GraalVM Native Image.

This repository is continually updated with new workshops and tutorials added frequently. Please check in regularly
to keep up to date.

To find out more about GraalVM, take a look at our website: [GraalVM](https://www.graalvm.org).

[Oracle's Luna Labs - Search GraalVM](https://luna.oracle.com), to try these workshops out online. Search for **graalvm**

## Using the Workshops and Tutorials

All of the workshops are written so that they will work on your own device (laptop, server). Each workshop begins with
prerequisites that you need to have installed. Please make sure you read these.

First, clone this repository:

```bash
git clone https://github.com/graalvm/workshops.git
```

Open a terminal, change directory to the workshop / tutorial you want to try out. Read the `README.md` file.
The workshops are organized into directories as follows:

* `native-image` : This collects all workshops related to GraalVM Native Image

We will add more directories as we add more content including ones relating to `native-image`, `polyglot` and using
GraalVM as a Java runtime.

## Native Image Workshops and Tutorials

### ![Lab Flask](./images/lab-flask.png) [GraalVM Native Image Quick Start](native-image/graalvm-native-image-quick-start/)
<a href="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-quickstart.yml">
<img alt="native-image-quickstart" src="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-quickstart.yml/badge.svg" /></a>

>  Workshop to gently introduce you to working with GraalVM Native Image. In the workshop you will:
>  - Build and run a Java application, using GraalVM
>  - Turn a Java application into a native executable, using GraalVM Native Image
>  - Build a native executable that works with the dynamic features of Java
>  - Use the Maven GraalVM plugin to build a native executable using GraalVM Native Image2.

### ![Lab Flask](./images/lab-flask.png) [Understanding Reflection with GraalVM Native Image](native-image/reflection/)
<a href="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-reflection.yml">
   <img alt="native-image-quickstart" src="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-reflection.yml/badge.svg" /></a>

>  Workshop that will help you understand how reflection works within GraalVM Native Image. In the workshop you will: 
>  - Use the `native-image` tool to compile a Java application that uses reflection into a native executable file
>  - Learn about the assisted configuration tooling provided by GraalVM

### ![Lab Flask](./images/lab-flask.png) [GraalVM Native Image Build Tools, for Maven](native-image/native-build-tools/)
<a href="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-build-tools-maven.yml">
   <img alt="native-image-quickstart" src="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-build-tools-maven.yml/badge.svg" /></a>

>  Workshop where you will:
>  - Learn about the _GraalVM Native Image Build Tools for Maven_

### ![Lab Flask](./images/lab-flask.png) [GraalVM Native Image, Spring & Containers](native-image/containerisation/)
<a href="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-containerisation.yml">
   <img alt="native-image-quickstart" src="https://github.com/graalvm/workshops/actions/workflows/github-actions-native-image-containerisation.yml/badge.svg" /></a>

>  Workshop introducing using GraalVM Native Image in containers. In this workshop you will:
>  - Add a basic Spring Boot application to a Docker Image and run it
>  - Build a native executable from this application, using GraalVM Native Image
>  - Add the native executable to a Docker Image
>  - Shrink your application docker image size with GraalVM Native Image and Distroless containers 

### ![Lab Flask](./images/lab-flask.png) [Non-Fungible Verses](native-image/non-fungible-verses/)

>  Workshop that builds upon the containerisation workshop. It adds:  
> 
>  - An introduction to some of the capabilities of the Oracle Cloud Infrastructure (OCI) always free tier.
>  - Showw how using [GraalVM Native Image](https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/) can help you make the most of your free tier allocation

### ![Lab Flask](./images/lab-flask.png) [VS Code Tools for Micronaut](/vscode-tools-for-micronaut/README.md)

>  A workshop that introduces the support for Micronaut&reg; available within VS Code when using the Tools for Micronaut extension.
> 
>  - Create a Micronaut application within VS Code.
>  - Use the code creation automation.
>  - Discover what the Micronaut Activity view can do:
>  - Work with an Oracle Database.
>  - Use VisualVM from within VS Code.
>  - Work with cloud resources in Oracle Cloud Infrastructure (OCI).

### ![Lab Flask](./images/lab-flask.png) [Using the Micronaut Expression Language within VS Code Tools for Micronaut](/vscode-micronaut-expression-language/README.md)

>  A workshop that introduces the support for Micronaut&reg; Expression Language available within VS Code when using the Tools for Micronaut extension.


