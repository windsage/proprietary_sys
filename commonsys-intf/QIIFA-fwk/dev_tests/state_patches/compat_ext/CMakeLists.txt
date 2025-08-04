


cmake_minimum_required(VERSION 3.5)

project(qiifatest VERSION 1.0.0 LANGUAGES CXX)

set(LIB_NAME "loremipsum")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/symbols/vendor/lib64)
set(CMAKE_BUILD_TYPE  "Debug")

#Bring the headers, such as Student.h into the project
include_directories(source/include)

#However, the file(GLOB...) allows for wildcard additions:
file(GLOB SOURCES "source/*.cpp")

#Generate the shared library from the sources
add_library(loremipsum SHARED ${SOURCES})

