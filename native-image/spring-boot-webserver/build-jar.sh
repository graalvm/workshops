#!/bin/sh

# Distroless Java Base-provides glibc and other libraries needed by the JDK
docker build . -f Dockerfile.debian-slim.uber-jar -t webserver:debian-slim.jar