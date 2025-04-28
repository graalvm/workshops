#!/bin/sh

# For local building
# ./mvnw -Dmaven.test.skip=true -Pnative,mostly-static native:compile

# Distroless Base (provides glibc)
docker build --no-cache . -f Dockerfile.distroless-base.mostly -t webserver:distroless-base.mostly-static