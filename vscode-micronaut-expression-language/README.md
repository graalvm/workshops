
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="images/GraalVM-rgbrev.png">
  <source media="(prefers-color-scheme: light)" srcset="images/GraalVM-rgb.png">
  <img alt="GraalVM logo" src="images/GraalVM-rgb.png" width="40%" style="margin-left: -50;">
</picture>

# ![Lab Flask](../images/lab-flask.png) Using the Micronaut Expression Language within VS Code Tools for Micronaut

In this Lab we will introduce you to the Micronaut&reg; tooling available within VS Code and how this can support the [Micronaut expression Language](https://docs.micronaut.io/latest/guide/#evaluatedExpressions). This tooling is very feature-rich and makes building and working with Micronaut applications easy. Let's get started.

Before attempting this Lab it is important that you do the core [VS Code tools for Micronaut Lab](../vscode-tools-for-micronaut/README.md). If you haven't looked at the core Lab yet, do it now and come back.

**Estimated Lab time: 30 minutes**

## Lab Objectives

During this lab, we will introduce you to the support for the [Micronaut expression Language](https://docs.micronaut.io/latest/guide/#evaluatedExpressions) available within the Tools for Micronaut extension for VS Code. This extension supports working with and building Micronaut applications. You will:

- Use the [Micronaut expression Language](https://docs.micronaut.io/latest/guide/#evaluatedExpressions) within an application.
- Have been shown the code-completion support for the Micronaut Expression Language within the Tools for Micronaut extension.

>Note: If you see the laptop icon in the lab, this means you need to do something, such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to update a file or run a shell command.
```

>Note: If you see the keyboard icon ![keyboard](./images/keyboard.jpg) in the lab, this means you are being given some tasks to try. Keep an eye out for it.

>Note: Please do keep a note of any suggestions for improvements, bugs, or features that you think are missing. We will be asking you along the way to make a note of these.

## Lab Prerequisites

Before starting this lab, you must have:

* A recent install of VS Code. If you don't have it, you can [download it here](https://code.visualstudio.com/download).
* An install of the most recent version of Oracle GraalVM, JDK17. Please read the GraalVM documentation for more details: [Download Oracle GraalVM](https://www.graalvm.org/downloads/).

## Supporting Documentation

The following is a set of documentation that may prove useful when completing this lab. Please do consult the docs if anything iss unclear.

* [Tools for Micronaut Extension for VS Code.](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut-tools)
* [Launch for Micronaut Extension for VS Code.](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut)

## Completed Project

The code for the completed project can be found in the directory: `vscode-tools-for-micronaut/demo`. It is recommended to follow along with the lab, but if you get stuck at any time the code is available.

## 1 - Install the Tools for Micronaut Extension

For this Lab you will need to install the following extensions into VS Code:

* [MS Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack).
* [Tools for Micronaut® Framework](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut-tools).
* [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client).

You can install it directly from within the VS Code marketplace, using the links above, or through the extensions activity view within VS Code.

> **Important:** If you already have these extensions installed, you will need to update to the latest versions.

## 2 - Create a Micronaut Application

We start this Lab by creating a simple Micronaut application. Throughout the Lab, we will add code to the application that will allow us to demonstrate support for the Micronaut Expression Language. Let's do that now.

An application is created using the `Micronaut Launch: Create Micronaut Project` action within the VS Code Command Palette. The Command Palette is opened with:

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
Mac:     ⌘ + Shift + P
Windows: Ctrl + Shift + P
Linux:   Ctrl + Shift + P
```

Then search for and run, `Micronaut Launch: Create Micronaut Project`.

The wizard will walk you through the process of creating a Micronuat application. You will need to create an application with the following properties:

* Version 4.6.2
* Application Type: Micronaut Application
* Java runtime: GraalVM JDK17
* Project name: demo
* Base package: com.example
* Language: Java (our tools focus on Java support)
* Project features: HTTP Client
* Build tool: You choose
* Test framework: JUnit

Save the project and open it in VS Code. You can add it to the current workspace, which will allow you to keep this guide open if you have opened it within VS Code.

## 3 - Use the Micronaut Expression Language

The tooling supports code completion within Expression Language strings and will see an example of this as we build up the code for our application. We will build a small application that uses the `@Scheduled` annotation to create a scheduled event. We will see how we can use the tooling to help provide auto-completion for Expression Language statements.

![keyboard](./images/keyboard.jpg)

Over to you:
* Create a new Java package within your application: `com.example.jobs`.
* Create a new Java class in the `com.example.jobs` package (remember that you can create a Controller through the Mcironaut menu within the right-click context menu - right-click, or `ctrl + click`, on the package to bring up the context menu): `Job.java`.

Next, we will need to create a scheduled job class which will contain the following code:

```java
package com.example.jobs;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

@Singleton
public class Job {
    private boolean jobRan = false;
    private boolean paused = true;


    @Scheduled(
        fixedRate = "1s")
    void run() {
        System.out.println("Job Running");
        this.jobRan = true;
    }

    public boolean isPaused() {
        return paused;
    } // (2)

    public boolean hasJobRun() {
        return jobRan;
    }

    public void unpause() {
        paused = false;
    }

    public void pause() {
        paused = true;
    }

}
```

This class represents a scheduled job that will run once every second, and when it runs, it will output a message to `stdout`. We haven't yet used the expression language support, but we will soon when we add a condition to the job. But first, let's get the job up and running and tested.

![keyboard](./images/keyboard.jpg)

Over to you:
* Replace the contents of the `Job.java` file with the code from the above code snippet.
* Run the application using the Micronaut Activity view. Do you see the task generating output in the terminal window?
* Stop your application.

The scheduled job runs. We will now add a condition to the scheduled job, using the Micronaut Expression Language, that stops it from running. The job has a `paused` variable, which it currently ignores. We will use this variable to control whether the task runs or not.

```java
    @Scheduled(
        fixedRate = "1s",
        condition = "#{}"
        )
    void run() {
        System.out.println("Job Running");
        this.jobRan = true;
    }
```

![keyboard](./images/keyboard.jpg)

Over to you:
* Update the `Job.java` file so that the `run()` method looks identical to above.
* Place your cursor inside the `#{}` on the value of the condition.
* Use the code completion that is now available to add the following expression. Code completion will work in the same way as everywhere else: 
    > `#{!this.paused}`
* Run the application using the Micronaut Activity view. The output from the scheduled task should no longer be seen.
* Stop your application.

And finally, to finish off the application, we need to add a means of changing the paused state of our scheduled task. We will do this using a controller, which when the endpoint in it is hit will set the paused state to false.

The following code is what we will use to do this:

```java
package com.example.controllers;

import com.example.jobs.Job;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

@Controller("/runtask")
public class RunTaskController {

    // The scheduled task singleton
    @Inject
    Job job;

    @Get(produces = "text/plain")
    public String activateTask() {
        job.unpause();
        return "Task activated.";
    }

}
```

![keyboard](./images/keyboard.jpg)

Over to you:
* Create a new Java package within your application: `com.example.controllers`.
* Create a new Java class in the `com.example.controllers` package (remember that you can create a Controller through the Mcironaut menu within the right-click context menu - right-click, or `ctrl + click`, on the package to bring up the context menu): `RunTaskController.java`.
* Replace the contents of the new controller, `RunTaskController.java` with the code snippet above.
* Run the application using the Micronaut Activity view.
* Open the `ENDPOINTS` panel in the Micronaut Activity View. Click on the new endpoint, `/runtask`. Call it using the REST Query Composer window. What do you see in the application output in the terminal window?
* Stop your application.
* What else can you do with the Expression Language support? Where else would you use it? Please make a note.


## Summary

We hope you have enjoyed this Lab and learnt a few things along the way. Although we have only touched upon what is possible with the Tools for Micronaut you have seen some of the many benefits offered.

Thanks for taking the time to do this lab!

Micronaut® is a registered trademark of Object Computing, Inc. Use is for referential purposes and does not imply any endorsement or affiliation with any third-party product.