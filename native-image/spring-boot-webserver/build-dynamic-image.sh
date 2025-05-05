#!/bin/sh

# For local building
# ./mvnw -Dmaven.test.skip=true native:compile

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build --no-cache . -f Dockerfile.distroless-java-base.dynamic -t webserver:distroless-java-base.dynamic