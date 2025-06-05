#!/bin/sh

# Distroless Java 21 (Debian)-provides glibc and other libraries needed by the JDK
docker build --no-cache . -f Dockerfile.distroless-java-base-jar -t webserver:distroless-java-base.jar