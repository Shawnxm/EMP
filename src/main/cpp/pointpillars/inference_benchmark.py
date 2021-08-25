# -*- coding: utf-8 -*-
# Filename : inference_benchmark
__author__ = 'Xumiao Zhang'


import sys
import os
import time
import numpy as np
os.environ['CUDA_VISIBLE_DEVICES'] = '3'
sys.path.append("second.pytorch/second/edge/")
from inference import Inference

if __name__ == '__main__':
	pathPrefix = "../../../test/resources/benchmark_data/2011_09_26/2011_09_26_drive_";
	setNames = ['0001_sync/velodyne_points/data/', 
				'0002_sync/velodyne_points/data/', 
				'0005_sync/velodyne_points/data/', 
				'0009_sync/velodyne_points/data/', 
				'0011_sync/velodyne_points/data/', ]
	filenames = []
	times = []
	
	for i in range(5):
		for file in os.listdir(pathPrefix+setNames[i]):
			filenames.append(pathPrefix+setNames[i]+file)

	inf = Inference()
	inf.read_config()
	inf.build_model()

	for file in filenames:
		print(file)
		points = np.fromfile(file, dtype=np.float32, count=-1).reshape([-1, 4])    
		t1 = time.time()
		result_str = inf.execute_model(points)
		t2 = time.time()
		times.append(t2-t1)

	print(f'Number of point cloud samples: {len(filenames)}')
	print(f'Avg. inference time: {np.mean(times[1:])*1000} ms, stddev: {np.std(times[1:])*1000}')
