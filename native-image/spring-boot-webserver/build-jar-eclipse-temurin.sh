#!/bin/sh

# Eclipse-temurin:25
docker build --no-cache . -f Dockerfile.eclipse-temurin-jar -t webserver:eclipse-temurin-jar
