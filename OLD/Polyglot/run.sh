#!/usr/bin/env bash
#
# You need to update the library path
#
export LD_LIBRARY_PATH=./jni/libtensorflow_framework.so:./jni/libtensorflow_framework.so.1
#

#
echo "Usage : pass in the path to an image"

# Image classification example
# Note the use of JNI
# This relies on the LD_LIBRARY_PATH being updated with access to the TensorFlow Linux Shared libs
# * ./jni/libtensorflow_framework.so
#
java -cp jars/libtensorflow-1.14.0.jar:target/classes \
     -Djava.library.path=./jni org.tensorflow.examples.LabelImage ./models $1
