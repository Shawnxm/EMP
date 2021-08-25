#!/bin/bash
mkdir -p lib
pushd lib
TORCH_PATH=libtorch
if [ -d "$TORCH_PATH" ]; then
    echo "$TORCH_PATH exists."
else 
    echo "Downloading $TORCH_PATH.zip ..."
    wget https://download.pytorch.org/libtorch/cu101/libtorch-shared-with-deps-1.5.0.zip
    unzip -q libtorch-shared-with-deps-1.5.0.zip
    rm libtorch-shared-with-deps-1.5.0.zip
fi
export LIBTORCH_HOME="$PWD/libtorch"
export USE_LIBTORCH_NIGHTLY=1
popd
