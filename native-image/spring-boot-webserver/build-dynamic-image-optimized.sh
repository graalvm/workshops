#!/bin/sh

# Compile with fully dynamically linked shared libraries; optimize for size
./mvnw -Dmaven.test.skip=true -Pdynamic-size-optimized native:compile

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build . -f Dockerfile.distroless-java-base.dynamic-optimized -t webserver:distroless-java-base.dynamic-optimized