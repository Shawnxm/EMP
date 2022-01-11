# EMP
EMP is an edge-assisted multi-vehicle perception system for connected and autonomous vehicles (CAVs), designed for efficient and scalable sensor data sharing under potentially fluctuating network conditions to enhance the CAVs' local perception. The code is used in our MobiCom '21 paper, [EMP: Edge-assisted Multi-vehicle Perception](https://dl.acm.org/doi/abs/10.1145/3447993.3483242).

### Setup EMP
Setup PointPillars (please first setup the Python environment following this [README](https://github.com/Shawnxm/second.pytorch)) and Draco.
```bash
cd <src_root>/src/main/cpp/
./setup_pointpillars.sh  # Download PointPillars source code
./build_pointpillars.sh  # Build PointPillars library

cd <src_root>/src/main/cpp
./setup_draco.sh  # Download Draco source code
./build_draco.sh  # Build Draco library
```
Build the JNI native library. 
```bash
cd <src_root>/
./build_native_lib.sh
```
You can directly run the system using IDE like IntelliJ IDEA or generate `.jar` file for running in a terminal.
```bash
./gradlew shadowjar
```

### Run EMP
#### Edge
First start an edge instance. 
Change the algorithm index (1-4) to run different partitioning algorithm:
1. No partitioning.
2. Naive partitioning based on the Voronoi Diagram.
3. Bandwidth-aware partitioning based on the Power Diagram.
4. REAP partitioning.
```bash
java -Djava.library.path=build/src/main/cpp \
   -Dlog4j.configurationFile=src/main/resources/log4j2-config.xml \
   -cp build/libs/emp-1.0.jar org.emp.edge.EdgeServer \
   -p [server port] -t [number of threads] -a [algorithm index] -c [number of clients] (-s [save path])
```

#### Vehicle
Then start vehicle instances in different terminals/machines. 
The vehicle type corresponds to the algorithm index (1: VehicleFull, 2: VehicleNaive, 3: VehicleBW, 4: VehicleReap). In the dataset object-0227, the available vehicle IDs are 1 (ego-vehicle), 4354, 4866, 5378, 21506, 22786.
```
java -Djava.library.path=build/src/main/cpp \
   -Dlog4j.configurationFile=src/main/resources/log4j2-config.xml \
   -cp build/libs/emp-1.0.jar org.emp.vehicle.[vehicle type] \
   -i [server ip] -p [server port] -c [client port] \
   -v [vehicle ID] -d [data path] -r [frame rate]
````  

### Perception
We modified [PointPillars](https://github.com/Shawnxm/second.pytorch) and use it as the perception module in EMP evaluation.

### Data
We modified [DeepGTAV - PreSIL](https://github.com/Shawnxm/DeepGTAV-PreSIL/tree/modified_for_emp) to create our synthetic multi-vehicle dataset from GTA V. The dataset contains vehicle sensor data (camera images, LiDAR point clouds, position/direction information) and labels.

| Data type          | Description                                                        | Format                                             |
|:------------------:|--------------------------------------------------------------------|----------------------------------------------------|
| image_2            | Ego vehicle's front camera data                                    | |
| velodyne           | Ego vehicle's LiDAR data                                           | x, y, z, object ID (if the point is in a object such as vehicle or pedestrian) |
| oxts               | Ego vehicle's position/direction information in the KITTI format   | latitude, longitude, altitude (KITTI world position), roll, pitch, heading|
| label_2            | Ground truth for surrounding objects                               | type, truncated, occluded, alpha, 2d bbox (left top, right bottom), dimensions (hwl), 3d bbox (xyz, camera coordinates), rotation_y (camera coordinates) |
| label_2_aug        | Augmented labels for surrounding objects                           | type, truncated, occluded, alpha, 2d bbox (left top, right bottom), dimensions (hwl), 3d bbox (xyz, camera coordinates), rotation_y (camera coordinates), object ID, points hit in 2D, points hit in 3D, speed, roll, pitch, object model, ID of vehicle the pedestrian is in |
| ego_object         | Augmented label for the ego vehicle (some values set to constant)  | type, -1, -1, -1, -1 -1 -1 -1, dimensions (hwl), 0 0 0, -1, object ID, -1, -1, speed, -1, -1, object model, 0 |

### Reference
Please cite our work if it helps your research:
> Xumiao Zhang, Anlan Zhang, Jiachen Sun, Xiao Zhu, Y. Ethan Guo, Feng Qian, and Z. Morley Mao. 2021. EMP: edge-assisted multi-vehicle perception. Proceedings of the 27th Annual International Conference on Mobile Computing and Networking. Association for Computing Machinery, New York, NY, USA, 545–558. DOI:https://doi.org/10.1145/3447993.3483242

### Contact
Please feel free to reach out (xumiao@umich.edu) for any questions about how to use the code or access to our dataset.