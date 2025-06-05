#!/bin/sh

# Eclipse-temurin:21
docker build --no-cache . -f Dockerfile.eclispe-temurin-jar -t webserver:eclispe-temurin-jar