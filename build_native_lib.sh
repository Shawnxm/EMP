#!/bin/bash
export DRACO_SRC_ROOT=${PWD}/src/main/cpp/draco
# Build benchmark
mkdir -p build
cd build
cmake -DCMAKE_MODULE_PATH=$DRACO_SRC_ROOT/install/lib/draco/cmake ..
make
