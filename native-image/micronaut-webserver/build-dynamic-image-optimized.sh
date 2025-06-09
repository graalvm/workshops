#!/bin/sh

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build . -f Dockerfile.distroless-java-base.dynamic-optimized -t webserver:distroless-java-base.dynamic-optimized