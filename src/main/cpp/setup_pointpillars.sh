#!/bin/bash
pushd pointpillars
mkdir build
git clone git@github.com:Shawnxm/second.pytorch.git --depth 1
pushd second.pytorch
git clone git@github.com:facebookresearch/SparseConvNet.git --depth 1
pushd SparseConvNet; bash build.sh; popd
popd
