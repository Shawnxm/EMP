set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

cmake_minimum_required(VERSION 2.8 FATAL_ERROR)

project(draco_benchmark)

include_directories(../draco/install/include)
find_package(draco)
find_library(DRACO_LIBRARY NAMES draco dracodec dracoenc HINTS $ENV{DRACO_SRC_ROOT}/install/lib)

add_executable (draco_benchmark draco_benchmark.cpp)
target_link_libraries (draco_benchmark ${DRACO_LIBRARY})
