#!/bin/sh

# For local building
# ./mvnw -Dmaven.test.skip=true -Pdynamic-size-optimized native:compile

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build . -f Dockerfile.distroless-java-base.dynamic-optimized -t webserver:distroless-java-base.dynamic-optimized