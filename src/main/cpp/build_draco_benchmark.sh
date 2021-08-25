#!/bin/bash
export DRACO_SRC_ROOT=${PWD}/draco
# Build benchmark
cd benchmark
cmake -DCMAKE_MODULE_PATH=$DRACO_SRC_ROOT/install/lib/draco/cmake .
make
