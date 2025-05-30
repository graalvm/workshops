#!/bin/sh

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build --no-cache . -f Dockerfile.distroless-java-base.dynamic -t webserver:distroless-java-base.dynamic