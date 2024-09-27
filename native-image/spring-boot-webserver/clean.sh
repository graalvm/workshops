#!/bin/sh
set +e

./mvnw clean
rm -rf jlink-jre cp.txt
docker images webserver -q | grep -v TAG | awk '{print($1)}' | xargs docker rmi