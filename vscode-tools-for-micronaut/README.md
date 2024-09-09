
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="images/GraalVM-rgbrev.png">
  <source media="(prefers-color-scheme: light)" srcset="images/GraalVM-rgb.png">
  <img alt="GraalVM logo" src="images/GraalVM-rgb.png" width="50%">
</picture>


# ![Lab Flask](../images/lab-flask.png) VS Code Tools for Micronaut

In this lab we will introduce you to the Micronaut&reg; tooling available within VS Code. This tooling is very feature rich and makes building and working with Micronaut applciations easy. Let's get started.


**Estimated lab time: 90 minutes**

## Lab Objectives

In this lab you we will introduce you to the Tools for Micronaut extension for VS Code. This extension supports working and building Micronaut application. You will:

- Install VS Code and the Tools for Micronaut extension
- Create a Micronaut application within VS Code

- Naviagte around your application

- Code creation Automation
  - Create a REST controller
  - Autocomplete within properties files

- Discover what the Micronaut Activity view can do
  - Use the Micronaut Activity View to run the application 
  - Naviagte around the end points and beans
  - Make a REST call
  - Add the Micronaut Control Panel to your application
  - Monitor & manage your application in realtime

- Use the Micronaut Expression Language

- Work with an Oracle Database
  - Connect to a database
  - Create Micronaut Data entities, repositories from and REST controllers to expose an existing database schema
  - Run youe application using the attached database
  - Generate tests for your controllers

- Use Visual VM from within VS Code
  - Install Visual VM from within VS Code
  - Use the integrated Visual VM support to solve performance issues in your application

- Work with cloud resources in Oracle Cloud Infrastructure (OCI)
  - Learn how to work with Oracle Cloud (OCI) resources within your application
  - Add an Object Storage bucket to your application
  - Run the application using the attached Object Storage Bucket

>Note: If you see the laptop icon in the lab, this means you need to do something such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something.
```

## Lab Prerequisites

Before starting this lab, you must have:

* A recent install of VS Code, if you don't you can [download if here](https://code.visualstudio.com/download).
* Install of most recent version of Oracle GraalVM. This can be done using the command line tool, `sdkman`. Please ateka  look at thsi for more details: [Download Oracle GraalVM](https://www.graalvm.org/downloads/).
* An Oracle ATP instance with the HR schema installed. for this labe we will provide you with a pre-configured instance that you can use. We will supply the details later on in the lab.

## Install the Tools for Micronaut Extension

In order to get all of the Micronaut features within VS Code, that we will look at in this lab, you first need to install the extension. 

You will need to install the following extensions into VS Code:

* [MS Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack).
* [tools for Micronaut® Framework](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut-tools).

You can either install directly from within the VS Code marketplace, links above, or through the extensions activity view within VS Code.

## Create a Micronaut Application

We start this lab by creating a simple Micronaut application and seeing how our tooling can progressively support us in adding features to it. We will also see how easy it is to run and interact with our application using our tools, but the first step is to create an application. Let's do that now.

An application can be created using the 

## Navigate Around your Application

## Code Creation Automation

### Create a REST controller

### Autocomplete within properties files

## Discover what the Micronaut Activity View Can Do

### Use the Micronaut Activity View to run the application 
### Naviagte around the end points and beans
### Make a REST call
### Add the Micronaut Control Panel to your application
### Monitor & manage your application in realtime

## Use the Micronaut Expression Language

## Work with an Oracle Database

### Connect to a database
### Create Micronaut Data entities, repositories from and REST controllers to expose an existing database schema
### Run youe application using the attached database
### Generate tests for your controllers

## Use Visual VM from within VS Code

### Install Visual VM from within VS Code
### Use the integrated Visual VM support to solve performance issues in your application

## Work with Cloud Resources in OCI

### Learn how to work with Oracle Cloud (OCI) resources within your application
### Add an Object Storage bucket to your application
### Run the application using the attached Object Storage Bucket

## Summary

We hope you have enjoyed this lab and learnt a few things along the way. You have seen the variety of benefits offered by the Tools for Micronaut extension. These include:

* The ability to monitor and manage you rapplication from within VS Code.
* Easy code creation for a host of mundane tasks.
* Easily connecting to and working with databases.
* Creating complete REST APIs from existing database schemas with 

## Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)

Micronaut® is a registered trademark of Object Computing, Inc. Use is for referential purposes and does not imply any endorsement or affiliation with any third-party product.