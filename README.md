<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg" 
    alt="GraalVM logo" 
    width="200px"
    style="display: block; float: right;">

# GraalVM Workshops & Tutorials

This repository contains content to get you started working with GraalVM. Within it we have a number of labs & tutorials
that will guide you through getting started with and using GraalVM. We also have labs that will deep-dive into particular 
topics, such as how reflection and Class loading works within GraalVM Native Image.

This repository is constantly be developed and new labs & tutorials will be added regularly. Please check in regularly
to keep upto date with what is going on.

Check out the prohects website at, [GraalVM](https://github.com/graalvm/workshops.git). 

## Using these Labs

All of these labs have written so that will work on your own device (laptop, server). Each lab starts with a set of
prerequisites that state what you will need to have installed on your device in order to do the lab. Please make sure you
read these.

First, clone this repository locally:

```shell
git clone https://github.com/graalvm/workshops.git
```

Then chnage directory to the lab / tutorial you want to try out. The labs are organised into folders as follows:

* `native-image` : Collects all of the labs related to GraalVM Native Image

We will add more folders as we add more content - we plan to add more labs relating to `ntive-image`, `polyglot` and
GraalVM as a Java runtime.

## Native Image Labs & Tutorials

### First Steps

These are the labs & tutorials you should start out with:

1. [GraalVM Native Image Quick Start](./native-image/graalvm-native-image-quick-start/index.md)
2. [Understanding Reflection with GraalVM Native Image](./native-image/reflection/index.md)

### Next Steps

1. [GraalVm Native Image, Spring & Containers](./native-image/containerisation/index.md)

