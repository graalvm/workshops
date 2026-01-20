#!/bin/sh

# Eclipse-temurin:25
docker build --no-cache . -f Dockerfile.eclispe-temurin-jar -t webserver:eclispe-temurin-jar
