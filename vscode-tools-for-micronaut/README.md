
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="images/GraalVM-rgbrev.png">
  <source media="(prefers-color-scheme: light)" srcset="images/GraalVM-rgb.png">
  <img alt="GraalVM logo" src="images/GraalVM-rgb.png" width="50%">
</picture>


# ![Lab Flask](../images/lab-flask.png) VS Code Tools for Micronaut

In this lab we will introduce you to the Micronaut&reg; tooling available within VS Code. This tooling is very feature rich and makes building and working with Micronaut applciations easy. Let's get started.


**Estimated lab time: 90 minutes**

## Lab Objectives

During this lab, we will introduce you to the Tools for Micronaut extension for VS Code. This extension supports working and building Micronaut applications. You will:

- Install VS Code and the Tools for Micronaut extension.
- Create a Micronaut application within VS Code.
- Use the code creation automation:
  - Create a REST controller.
- Discover what the Micronaut Activity view can do:
  - Use the Micronaut Activity View to run the application.
  - Navigate around the application's endpoints and beans.
  - Make a REST call.
  - Add the Micronaut Control Panel to your application.
  - Monitor & manage your application in real-time.
- Use autocomplete within properties files.
- Use the Micronaut Expression Language.
- Work with an Oracle Database:
  - Connect to a database.
  - Create Micronaut Data entities, repositories from an existing database schema, and then REST controllers using the tooling.
  - Run your application using the attached database.
  - Generate tests for your controllers.
- Use Visual VM from within VS Code:
  - Install Visual VM from within VS Code.
  - Use the integrated Visual VM support to solve performance issues in your application.
- Work with cloud resources in Oracle Cloud Infrastructure (OCI):
  - Learn how to work with Oracle Cloud (OCI) resources within your application.
  - Add an Object Storage bucket to your application.
  - Run the application using the attached Object Storage Bucket.

>Note: If you see the laptop icon in the lab, this means you need to do something, such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something.
```

## Lab Prerequisites

Before starting this lab, you must have:

* A recent install of VS Code. If you don't have it, you can [download it here](https://code.visualstudio.com/download).
* An install of the most recent version of Oracle GraalVM. Please read the GraalVM documentation for more details: [Download Oracle GraalVM](https://www.graalvm.org/downloads/).
* An install of the [Oracle Cloud CLI](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/cliinstall.htm).
* An Oracle [Autonomous Transaction Processing](https://www.oracle.com/uk/autonomous-database/autonomous-transaction-processing/) (ATP) instance with the HR schema installed. If you can not create an ATP instance and install the schema then we can provide you with a pre-configured instance.
* The Oracle sample [HR schema can be found here](https://github.com/oracle-samples/db-sample-schemas). This link also contains instructions for installing. 

## Install the Tools for Micronaut Extension

For this lab you will need to install the following two extensions into VS Code:

* [MS Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack).
* [tools for Micronaut® Framework](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut-tools).

You can either install directly from within the VS Code marketplace, links above, or through the extensions activity view within VS Code.

## Create a Micronaut Application

We start this lab by creating a simple Micronaut application and seeing how our tooling can progressively support us in adding features to it. We will also see how easy it is to run and interact with our application using the Tools for Micronaut, but the first step is to create an application. Let's do that now.

An application can be created using the `Micronaut Launch: Create Micronaut Project` action within the VS Code Command Pallette. The Command Pallette can be opened with:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
Mac: ⌘ + Shift + P
Windows: Ctrl + Shift + P
Linux: Ctrl + Shift + P
```

Then search for and run, `Micronaut Launch: Create Micronaut Project`.

The wizard will walk you through the process of creating a Micronuat application. You will need to create an application with the following properties:

* Version 4.6.1
* Application Type: Micronaut Application
* Java runtime: GraalVM JDK21
* Project name: demo
* Base package: com.example
* Language: Java (our tools focus on Java support)
* Project features: Micronaut Data JDBC, Oracle Cloud Autonomous Transaction Processing (ATP), HTTP Client
* Build tool: You choose
* Test framework: JUnit

Save the project and open it in VS Code. You can choose to add it to the current workspace, which will allow you to keep this guide open if you have opened it within VS Code.

Initially we won't be using the Micronaut Data and Oracle ATP modules, but creating them now will mean that adding database functionality to our application will be easier later on.

Before we do anything else, we will need to comment out all of the `datasources.*` properties in the application's `src/main/resources/application.properties`.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```properties
#Mon Sep 09 15:24:38 UTC 2024
#datasources.default.dialect=ORACLE
#datasources.default.dialect=UTC
micronaut.application.name=demo
#datasources.default.ocid=
#datasources.default.schema-generate=CREATE_DROP
#datasources.default.walletPassword=
oci.config.profile=DEFAULT
```

## Code Creation

Tools for Micronaut has support for creating Micronaut classes from templates. Within the right click context menu, also launched by `CTRL + Mouse-Click`, you can see a submenu that groups all of the Micronuat context specific actions.

<img alt="Micronaut context actions menu" src="./images/vscode-context-menu-micronaut-menu.png" width="50%">

We will be using the items in this menu to easily create classes for us.

## Create a REST controller

Let's start by creating a REST based controller. We will first create a new Java package, within our base package, to hold our controller. A new package can be easily created with the VS Code context action menu, as shown below.

<img alt="Create a new Java package" src="./images/vscode-context-menu-new-package.png" width="50%">

Once you have created the package for holding the controllers, create a new controller with the nam, `PingController`. You should see the following, or similar if you changed the name.

```java
@Controller("/ping")
public class PingController {

    @Get(produces = "text/plain")
    public String get() {
        // TODO: review the generated method skeleton and provide a meaningful implementation.
        return "Example Response";
    }
}
```

You can trigger code completion, Intellisense, suggestions within VS Code using the key combination: `CTRL + SPACE`.

![Triggering Intellisense in the editor](images/intellisense-gif.gif)

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)

Over to you:
* Add a `@POST` method to the controller you just created. It doesn't need to do anything with the payload.
* What other methods can be added to the controller?
* What code completions, code generations would you like to see added?

## Discover what the Micronaut Activity View Can Do

The Tools for Micronaut extension aupports a custom IDE view dedicated to Micronaut. When you install the extension the icon for this view is visible on the right activity bar, as shown below. It can be shown, or hidden, by right clicking (`CTRL + mouse click` on MacOS) on the activity bar and either activating, or deactivating it.

![Open the Micronaut Activity View](images/micronaut-activity-view.gif)

We will now use the Micronaut Activity View to run and interact with the application that we have been creating.

### Use the Micronaut Activity View to run the application 
### Naviagte around the end points and beans
### Make a REST call
### Add the Micronaut Control Panel to your application
### Monitor & manage your application in realtime

## Autocomplete within properties files

TODO: Named datasources will be coming in a future release.


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