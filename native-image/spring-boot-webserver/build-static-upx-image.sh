#!/bin/sh

# For local building
# ./upx --lzma --best -o target/webserver.static-upx target/webserver.static

# Scratch--fully static and compressed
docker build --no-cache . -f Dockerfile.scratch.static-upx -t webserver:scratch.static-upx