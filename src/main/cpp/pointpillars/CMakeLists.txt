cmake_minimum_required(VERSION 2.8)

cmake_policy(SET CMP0042 OLD)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories($ENV{HOME}/anaconda3/envs/pointpillars/lib/python3.7/site-packages/numpy/core/include/)
include_directories($ENV{HOME}/anaconda3/envs/pointpillars/include/python3.7m/)

find_library(python3.7m $ENV{HOME}/anaconda3/envs/pointpillars/lib/)

link_directories($ENV{HOME}/anaconda3/envs/pointpillars/lib/)

add_executable(inference_bin inference.cpp)
add_library(inference SHARED inference.cpp)

target_link_libraries(inference python3.7m)
target_link_libraries(inference_bin python3.7m)
