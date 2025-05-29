#!/bin/sh

# Distroless Base (provides glibc)
docker build --no-cache . -f Dockerfile.distroless-base.mostly -t webserver:distroless-base.mostly-static