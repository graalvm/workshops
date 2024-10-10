#!/bin/sh 

./build-jar.sh
./build-jlink.sh
./build-dynamic-image.sh
./build-mostly-static-image.sh
./build-static-image.sh
./build-static-upx-image.sh

echo "Generated Executables"
ls -lh target/webserver*

echo "Generated Docker Container Images"
docker images webserver