#!/bin/sh

# Compile with fully dynamically linked shared libraries
./mvnw -Dmaven.test.skip=true native:compile

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build . -f Dockerfile.distroless-java-base.dynamic -t webserver:distroless-java-base.dynamic