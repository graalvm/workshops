#!/bin/sh

# Eclipse Temurin Java 25
docker build --no-cache . -f Dockerfile.eclipse-temurin-jar -t webserver:eclipse-temurin-jar