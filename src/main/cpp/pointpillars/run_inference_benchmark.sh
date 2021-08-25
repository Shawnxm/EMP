#!/bin/bash
source ~/anaconda3/bin/activate pointpillars
python -W ignore inference_benchmark.py > results.log
source ~/anaconda3/bin/deactivate pointpillars

