# EMP
EMP is an edge-assisted multi-vehicle perception system for connected and autonomous vehicles (CAVs), designed for efficient and scalable sensor data sharing under potentially fluctuating network conditions to enhance the CAVs' local perception. The code is used in our MobiCom '21 paper: EMP: Edge-assisted Multi-vehicle Perception.

### Setup
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

### Run
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

If there are any questions, please feel free to reach out (xumiao@umich.edu).