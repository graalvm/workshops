<img src="https://www.graalvm.org/resources/img/home/logo_mobile_openmenu.svg"
alt="GraalVM logo"
width="200px">

# VS Code Tools for Micronaut

In this lab we will introduce you to the Micronaut tooling available within VS Code. This tooling is very feature rich and makes building and working with Micronaut applciations easy.


### blab intro


Estimated lab time: 60 minutes

## Lab Objectives

In this lab you will:

- Install VS Code and the Tools for Micronaut extension
- Create a Micronaut application within VS Code
- Discover the Micronaut Activity view 
  - Use the Micronaut Activity View to run the application 
  - Naviagte around the end points and beans
  - Add the Micronaut Control Panel to your application
  - Add support for monitoring your application in realtime
- Code creation
  - Create a REST controller
- Work with an Oracle Database
  - Connect to a database
  - Create Micronaut Data entities, repositories from and REST controllers to expose an existing database schema
  - Run youe application using the attached database
  - Generate tests for your controllers
- Use Visual VM from within your Micronaut project
  - Install Visual VM from within VS Code
  - Use the integrated Visual VM support to solve performance issues in your application
- Oracle Cloud Support
  - Learn how to work with Oracle Cloud (OCI) resources within your application
  - Add an Object Storage bucket to your application
  - Run the application using the attached Object Storage Bucket


- Create a Graal Development Kit Application?
- DevOps

>Note: If you see the laptop icon in the lab, this means you need to do something such as enter a command. Keep an eye out for it.

![](images/RMIL_Technology_Laptop_Bark_RGB_50.png#input)
```
# This is where you will need to do something
```

## Lab Prerequisites

Before starting this lab, you must have:

* 


### Summary

We hope you have enjoyed this lab and learnt a few things along the way. You've seen how to containerise
a Java application. Then you've converted that Java application into a native executable, which starts significantly 
faster than the Java application. You then containerised the native executable and have seen how the size of the 
Docker image, with your native executable in it, is much smaller than the Java Docker Image.

Finally, you saw how to build a mostly-statically linked native executable with Native Image. These can be
packaged in smaller containers, such as Distroless and these let you shrink the size of the Docker Image even further.

### Learn More

- Watch a presentation by the Native Image architect Christian Wimmer [GraalVM Native Image: Large-scale static analysis for Java](https://www.youtube.com/embed/rLP-8q3Cb8M)
- [GraalVM Native Image reference documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/reference-manual/native-image/)
