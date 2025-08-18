#!/bin/sh

./build-jar-java-base.sh
./build-jar-eclipse-temurin.sh
./build-jlink.sh
./build-dynamic-image.sh
./build-dynamic-image-optimized.sh
./build-mostly-static-image.sh
./build-static-image.sh
./build-alpine-static-image.sh
./build-static-upx-image.sh

echo "Generated Executables"
ls -lh target/webserver*

echo "Generated Docker Container Images"
docker images webserver