#!/bin/sh
set -e

wget -q https://github.com/upx/upx/releases/download/v4.2.4/upx-4.2.4-amd64_linux.tar.xz
tar -xJf upx-4.2.4-amd64_linux.tar.xz
rm -rf upx-4.2.4-amd64_linux.tar.xz
mv upx-4.2.4-amd64_linux/upx .
rm -rf upx-4.2.4--amd64_linux