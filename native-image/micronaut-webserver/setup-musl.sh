#!/usr/bin/env bash
set +e

# Download the `musl` toolchain that includes the `zlib` library.
# Extract it, and add to the system path:

curl -SLO https://gds.oracle.com/download/bfs/archive/musl-toolchain-1.2.5-oracle-00001-linux-amd64.tar.gz
tar xzf musl-toolchain-1.2.5-oracle-00001-linux-amd64.tar.gz
export PATH="$(pwd)/musl-toolchain/bin:$PATH"
rm -rf musl-toolchain-1.2.5-oracle-00001-linux-amd64.tar.gz