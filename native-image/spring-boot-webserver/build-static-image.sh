#!/bin/sh

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

TOOLCHAIN_DIR=${SCRIPT_DIR}/musl-toolchain
PATH=${TOOLCHAIN_DIR}/bin:${PATH}

# Create a statically linked binary that can be used without any additional library dependencies; optimize for size
./mvnw -Dmaven.test.skip=true -Pnative,fully-static native:compile

# Scratch-nothing
docker build . -f Dockerfile.scratch.static -t webserver:scratch.static