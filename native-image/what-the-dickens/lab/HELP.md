# Help Guide

This project has been configured to let you generate either a lightweight container or a native executable.
It is also possible to run your tests as a native code.

### Lightweight Container with Paketo Buildpacks

If you're already familiar with Spring Boot container images support, this is the easiest way to get started.
Docker should be installed and configured on your machine prior to running the steps.

To create the image, run the following goal:
```bash
./mvnw spring-boot:build-image -Pnative
```

Then, you can run the app like any other container:
```bash
docker run --rm what_the_dickens:0.0.1-SNAPSHOT
```

### Executable with Native Build Tools

Use this option if you want to explore more options such as running your tests as a native code.
[GraalVM 25](https://www.graalvm.org/downloads/) should be installed on your machine.

To create the executable, run the following goal:
```bash
./mvnw native:compile -Pnative
```

Then, you can run the application as follows:
```bash
./target/what_the_dickens
```

You can also run your existing tests suite as a native code.
This is an efficient way to validate the compatibility of your application.

To run your existing tests in a native image, run the following goal:
```bash
./mvnw test -PnativeTest
```

### Learn More

For further reference, check the following:

* [Build a Native Executable from a Spring Boot Application](https://www.graalvm.org/latest/reference-manual/native-image/guides/build-spring-boot-app-into-native-executable/)
* [Developing Your First GraalVM Native Application](https://docs.spring.io/spring-boot/how-to/native-image/developing-your-first-application.html)