cmake_minimum_required(VERSION 2.8)

cmake_policy(SET CMP0042 OLD)
project(EmpNative)

find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})

add_subdirectory(src/main/cpp)
add_subdirectory(src/test/cpp)
