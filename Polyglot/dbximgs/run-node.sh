#!/usr/bin/env bash

# This allows you to run the node app without the Java
#
# IMPORTANT : You need to update the controller in the node
# app to comment out the calls to the Java code

# If we want to make use of the Java code we need to:
# * Tell the node binary that we want JVM interop
# * Add our classes to the path - note the classpath params
# * We can do things like pass JNI paths
#
# NOTE: We are not using npm, but all `npm start` does is call the run script from the package.json
#
# Parameters that we can pass to GraalVM' sversion of node:
#
# --jvm : make java interop available
# --vm.cp : add to the classpath
# --vm.Djava.library.path : Set JNI "plugins"
#
echo "Starting site on http://localhost:3001"
# 2 ways to run node (native image / JIT)
# Note that we can pass all kinds of things through to the JVM
node --jvm --vm.cp=../jars/libtensorflow-1.14.0.jar:../target/classes --vm.Djava.library.path=../jni ./bin/www
