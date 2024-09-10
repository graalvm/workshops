
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="images/GraalVM-rgbrev.png">
  <source media="(prefers-color-scheme: light)" srcset="images/GraalVM-rgb.png">
  <img alt="GraalVM logo" src="images/GraalVM-rgb.png" width="40%" style="margin-left: -50;">
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
# This is where you will need to do update a file, o run a shell command.
```

>Note: If you see the keyboard icon, ![keyboard](./images/keyboard.jpg), in the lab, this means you are being given some tasks to try. Keep an eye out for it.

>Note: Please do keep a note of any suggestions for improvements, bugs, or for features that you think are missing. We will basking you along the way to make a note of these.

## Lab Prerequisites

Before starting this lab, you must have:

* A recent install of VS Code. If you don't have it, you can [download it here](https://code.visualstudio.com/download).
* An install of the most recent version of Oracle GraalVM. Please read the GraalVM documentation for more details: [Download Oracle GraalVM](https://www.graalvm.org/downloads/).
* An install of the [Oracle Cloud CLI](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/cliinstall.htm).
* An Oracle [Autonomous Transaction Processing](https://www.oracle.com/uk/autonomous-database/autonomous-transaction-processing/) (ATP) instance with the HR schema installed. If you can not create an ATP instance and install the schema then we can provide you with a pre-configured instance.
* The Oracle sample [HR schema can be found here](https://github.com/oracle-samples/db-sample-schemas). This link also contains instructions for installing. 


## Supporting Documentation

The following is a set of documentation that may prove useful when completing this lab. Please do consult the docs if anything iss unclear.

* [Tools for Micronaut Extension for VS Code.](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut-tools)
* [Launch for Micronaut Extension for VS Code.](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut)

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
Mac:     ⌘ + Shift + P
Windows: Ctrl + Shift + P
Linux:   Ctrl + Shift + P
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

Initially, we won't be using the Micronaut Data and Oracle ATP modules, but creating them now will mean that adding database functionality to our application will be easier later on.

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

<img alt="Micronaut context actions menu" src="./images/vscode-context-menu-micronaut-menu.png" width="40%">

We will be using the items in this menu to easily create classes for us.

## Create a REST controller

Let's start by creating a REST-based controller. We will first create a new Java package, within our base package, to hold our controller. This can easily be done within the VS Code context action menu, as shown below.

<img alt="Create a new Java package" src="./images/vscode-context-menu-new-package.png" width="40%">

Once you have created the package for holding the controllers, create a new controller with the name, `PingController`. You should see the following, or similar if you changed the name.

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

![keyboard](./images/keyboard.jpg)

Over to you:
* Add a `@POST` method to the controller you just created. It doesn't need to do anything with the payload.
* What other methods can be added to the controller?
* What code completions and code generations would you like to see added? Please make a note.

## Discover what the Micronaut Activity View Can Do

The Tools for Micronaut extension supports a custom IDE view dedicated to Micronaut. When you install the extension the icon for this view is visible on the right activity bar, as shown below. It can be shown, or hidden, by right-clicking (`CTRL + mouse click` on MacOS) on the activity bar and either activating or deactivating it.

![Open the Micronaut Activity View](images/micronaut-activity-view.gif)

We will now use the Micronaut Activity View to run and interact with the application that we have been creating.

### Use the Micronaut Activity View to run the application 

The first thing we can do in the Micronaut Activity View, is run our application. 

<img alt="Application View" src="./images/mn-activity-view-run.png" width="40%" >

In the first panel of the Micornaut Activity View we can see our application. By clicking on the various icons shown when we hover the mouse over the application name we can start, debug and stop our application.

![keyboard](./images/keyboard.jpg)

Over to you:
* Start the application, and then stop it, using the view.
* Add a breakpoint to your controller class and use the Micronaut Activity view to debug your application.
* Run the application in continuous mode, then change the text generated by the controller and see what happens.
* What other features and functionality would you expect to see? Please make a note.

### Naviagte around the end points and beans

Within the Micronaut Activity View, there is rich support for finding and querying beans and the HTTP endpoints of your application. We can see the panels shown in the Micronaut Activity View below.

<img alt="Application View" src="./images/mn-activity-view-beans-and-endpoints.png" width="40%" >

Within the `BEANS` panel, we can see the beans that have been declared within the application and those that come from dependencies. 

![keyboard](./images/keyboard.jpg)

Over to you:
* Use the `BEANS` panel to navigate to your Controller bean.
* Use the search and filter feature of the `BEANS` panel to locate some subset of beans that come from dependencies. Navigate to the source code for these beans.
* What other features would you like to see associated with the beans panel? Please make a note.

Within the `ENDPOINTS` panel we can see the endpoints that were defined within our controller. 

![keyboard](./images/keyboard.jpg)

Over to you:
* Use the `ENDPOINTS` view to navigate to the code defining the endpoints in the application.
* Start the application. What new features are availabl for the endpoint(s)?
* What other features would you like to see associated with endpoints panel? Please make a note.

There is one further way to navigate around your application in VS Code and that is using the, `Go to Symbol in Workspace` tool. This is opened with the key combination of, `CTRL + T` on Windows and Linux and `⌘  + T` on MacOS. When the symbols search field is opened we can use one of the two prefixes to filter the symbols to those that we want:

* `@/` to show all defined request mappings.
* `@+` to show all defined beans.

<img alt="Application View" src="./images/micronaut-navigation.png" width="60%" >

This functions in a similar manner to what we have already seen, but it is sometimes more convient.

![keyboard](./images/keyboard.jpg)

Over to you:
* Use the `Go to Symbol in Workspace` to locate your controllers and endpoints.

### Make a REST call

We hinted in the previous section that it was possible to do more than view the endpoints of your application. When your application is running, the endpoints panel will provide further icons that allow you to call an endpoint.

![keyboard](./images/keyboard.jpg)

* If your application isn't running at the moment, start it now.

The first of these new icons, shown below, will open the running endpoint in a browser.

<img alt="Application View" src="./images/mn-activity-view-endpoint-open-in-browser.png" width="25%" >


The second launches a REST composition tool that will allow you to call the endpoint with HTTP parameters and payloads.

<img alt="Application View" src="./images/mn-activity-view-controllers-compose-query.png" width="25%" >

By clicking on the, `Send Request`, text above the `curl`-like query string we can send data to the endpoint and we will see the response opened in a side window.

<img alt="Application View" src="./images/mn-send-rest-query-with-response.png" width="60%" >

![keyboard](./images/keyboard.jpg)

Over to you:
* Use the Compose REST Query editor to call your endpoint.
* Add another endpoint, using `POST`, and compose a query and call it.
* Are there any features you would like to see? Please make a note.

### Add the Micronaut Control Panel to your application

The [Micronaut Control Panel](https://micronaut-projects.github.io/micronaut-control-panel/snapshot/guide/) provides a web UI that allows you to view and manage the state of your Micronaut application. The Micronaut Activity View allows for integrating the Control Panel into your application. 

![keyboard](./images/keyboard.jpg)

* If your application is running, stop it now. We are not able to update the configuration of a running application.

To enable the control panel we can use the Application panel within the Micronaut Activity View. Click on the pencil icon to edit, choose `Enabled`, confirm updating the dependencies of your project and finally specify an environment. This is shown below.

<img alt="Enabling the Micronaut Control Panel" src="./images/mn-activity-view-enable-control-panel.gif" width="60%" >

With the control panel enabled restart your application from within the Application Panel of the Micronaut Activity View and go to the Control Panel. You can open the control panel by clicking on the link icon shown in the image below.

<img alt="Enabling the Micronaut Control Panel" src="./images/mn-activity-view-goto-control-panel.png" width="40%" >

![keyboard](./images/keyboard.jpg)

* Enable the Micronaut Control Panel for your application.
* Open and browse around the Control Panel for your application.
* Are there missing features for integrating with the Control Panel? Please make a note.

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