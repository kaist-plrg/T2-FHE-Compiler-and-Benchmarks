cmake_minimum_required(VERSION 3.13)

project(CLEAR_EXAMPLE LANGUAGES CXX)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/bin)

add_executable(test.out)
target_sources(test.out PRIVATE
  ${CMAKE_CURRENT_LIST_DIR}/compiled/test.cpp
)
