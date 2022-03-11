#!/usr/bin/env bash

# Use docker inspect to fetch thhe size of the container
# convert the size of the container, in bytes, into something useful (MBs)
#!/bin/bash
size_in_bytes=`docker inspect -f "{{ .Size }}" $1`
echo $((size_in_bytes/1024/1024))
