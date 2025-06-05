#!/bin/sh

rm -rf jlink-jre cp.txt

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build --no-cache . -f Dockerfile.distroless-java-base.jlink -t webserver:distroless-java-base.jlink