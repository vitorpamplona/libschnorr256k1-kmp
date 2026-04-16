cmake_minimum_required(VERSION 3.18)
project(bench_vs_acinq C)

# This benchmark is NOT part of the default build.
# It requires ACINQ's libsecp256k1-jni.so to be available.
#
# Usage:
#   mkdir -p build && cd build
#   cmake -DACINQ_LIB_DIR=/path/to/acinq/lib ..
#   make
#   ./bench_vs_acinq

# Build libschnorr256k1
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_BENCH OFF CACHE BOOL "" FORCE)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../libschnorr256k1
                 ${CMAKE_CURRENT_BINARY_DIR}/libschnorr256k1)

add_executable(bench_vs_acinq bench_vs_acinq.c)
target_link_libraries(bench_vs_acinq PRIVATE schnorr256k1)

# ACINQ library (optional, must be provided via -DACINQ_LIB_DIR=...)
if(ACINQ_LIB_DIR)
    target_link_directories(bench_vs_acinq PRIVATE ${ACINQ_LIB_DIR})
    target_link_libraries(bench_vs_acinq PRIVATE secp256k1-jni)
    set_target_properties(bench_vs_acinq PROPERTIES
        BUILD_RPATH ${ACINQ_LIB_DIR}
        INSTALL_RPATH ${ACINQ_LIB_DIR}
    )
else()
    message(WARNING "ACINQ_LIB_DIR not set — bench_vs_acinq will fail to link. "
                    "Pass -DACINQ_LIB_DIR=/path/to/acinq/libsecp256k1-jni.so directory.")
endif()
