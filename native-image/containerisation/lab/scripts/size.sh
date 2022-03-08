#!/usr/bin/env bash

# Use docker inspect to fetch thhe size of the container
# convert the size of the container, in bytes, into something useful (MBs)
docker inspect -f "{{ .Size }}" $1 | numfmt --to=si | sed 's/.$//'