# !/bin/sh

# For local building
# SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# TOOLCHAIN_DIR=${SCRIPT_DIR}/musl-toolchain
# PATH=${TOOLCHAIN_DIR}/bin:${PATH}

# Scratch-nothing
docker build --no-cache . -f Dockerfile.scratch.static -t webserver:scratch.static