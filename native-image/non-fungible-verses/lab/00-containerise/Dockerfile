# Base Image
FROM container-registry.oracle.com/graalvm/jdk:25

# Pass in the JAR file as an argument to the image build
ARG JAR_FILE

# This image will need to expose TCP port 8080, as this is the port on which your app will listen
EXPOSE 8080

# Copy the JAR file from the `target` directory into the root of the image
COPY ${JAR_FILE} app.jar

# Run Java when starting the container
ENTRYPOINT ["java"]

# Pass in the parameters to the Java command that make it load and run your executable JAR file
CMD ["-jar","app.jar"]