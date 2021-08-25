cmake_minimum_required(VERSION 2.8)
project(EmpNativeTest)

file(GLOB libEmpNativeTest_SOURCES *.cpp)
file(GLOB libEmpNativeTest_HEADERS *.h)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp)
link_directories(${JNI_LIBRARIES})
add_library(${PROJECT_NAME} SHARED ${libEmpNativeTest_SOURCES} ${libEmpNativeTest_HEADERS})
target_link_libraries(${PROJECT_NAME} EmpNative)

set(CMAKE_CXX_FLAGS "-std=c++11")
