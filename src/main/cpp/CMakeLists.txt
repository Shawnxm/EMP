cmake_minimum_required(VERSION 2.8)

cmake_policy(SET CMP0042 OLD)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

project(EmpNativeLib)

file(GLOB libEmpNative_SOURCES *.cpp)
file(GLOB libEmpNative_HEADERS *.h)

include_directories(draco/install/include)
include_directories($ENV{HOME}/anaconda3/envs/pointpillars/lib/python3.7/site-packages/numpy/core/include/)
include_directories($ENV{HOME}/anaconda3/envs/pointpillars/include/python3.7m/)
link_directories($ENV{HOME}/anaconda3/envs/pointpillars/lib/)
link_directories(${JNI_LIBRARIES})

find_package(draco)
find_library(DRACO_LIBRARY NAMES draco dracodec dracoenc HINTS $ENV{DRACO_SRC_ROOT}/install/lib)
find_library(PYTHON_LIBRARY NAME python3.7m HINTS $ENV{HOME}/anaconda3/envs/pointpillars/lib/)
add_library(EmpNative SHARED ${libEmpNative_SOURCES} ${libEmpNative_HEADERS})
target_link_libraries(EmpNative ${DRACO_LIBRARY} ${PYTHON_LIBRARY})
