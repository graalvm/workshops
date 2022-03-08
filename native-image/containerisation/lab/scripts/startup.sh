#!/usr/bin/env bash

# Grab from the docker logs of the container the startup time. Spring Boot apps output the time to startup into
# the logs. We look sfor this, extract it and clean it up using a Regular Expression
docker logs --tail 10 $(docker ps -q -f name=$1) | grep -Eo 'Started .+ in [0-9]+.[0-9]+ sec' | sed 's/[[:space:]]sec$//' | sed 's/Started[[:space:]][a-zA-Z]\+[[:space:]]in[[:space:]]//'
