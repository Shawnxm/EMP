#!/bin/bash
git clone --branch 1.3.6 https://github.com/google/draco.git --depth 1
pushd draco; git checkout -b local-v1.3.6; popd
pushd draco; mkdir build; mkdir install; popd
