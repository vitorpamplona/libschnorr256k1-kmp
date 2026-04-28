cmake_minimum_required(VERSION 3.18)
project(schnorr256k1_jni C)

# Build libschnorr256k1 as static library (no tests/bench)
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_BENCH OFF CACHE BOOL "" FORCE)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../libschnorr256k1
                 ${CMAKE_CURRENT_BINARY_DIR}/libschnorr256k1)

# JNI shared library
add_library(schnorr256k1_jni SHARED jni_bridge.c)
target_link_libraries(schnorr256k1_jni PRIVATE schnorr256k1)

# JNI bridge uses the public API header (via libschnorr256k1's PUBLIC include)
# No need for additional include paths for the library itself.

# JNI headers
if(ANDROID)
    find_library(log-lib log)
    if(log-lib)
        target_link_libraries(schnorr256k1_jni PRIVATE ${log-lib})
    endif()
elseif(JNI_INCLUDE_DIR)
    target_include_directories(schnorr256k1_jni PRIVATE ${JNI_INCLUDE_DIR})
    if(JNI_INCLUDE_DIR_PLATFORM)
        target_include_directories(schnorr256k1_jni PRIVATE ${JNI_INCLUDE_DIR_PLATFORM})
    endif()
else()
    find_package(JNI REQUIRED)
    target_include_directories(schnorr256k1_jni PRIVATE ${JNI_INCLUDE_DIRS})
endif()
