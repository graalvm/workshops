#!/bin/sh

# Scratch--fully static and compressed
docker build --no-cache . -f Dockerfile.scratch.static-upx -t webserver:scratch.static-upx