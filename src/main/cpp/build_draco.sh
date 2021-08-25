#!/bin/bash
export DRACO_SRC_ROOT=${PWD}/draco
export CXXFLAGS="-fPIC"
cd draco
cd build
cmake -DCMAKE_INSTALL_PREFIX=$DRACO_SRC_ROOT/install ..
make
make install
