cmake_minimum_required(VERSION 3.22)
project(deaftly_ppocr LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT DEFINED NCNN_ROOT)
    message(FATAL_ERROR "NCNN_ROOT must point at the extracted ncnn Android release")
endif()

set(ncnn_DIR "${NCNN_ROOT}/${ANDROID_ABI}/lib/cmake/ncnn")
find_package(ncnn REQUIRED)

add_library(deaftly_ppocr SHARED ppocr_engine.cpp ppocr_jni.cpp)
target_compile_options(deaftly_ppocr PRIVATE -O3 -fvisibility=hidden -Wall -Wextra)
target_link_options(deaftly_ppocr PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL -Wl,-z,max-page-size=16384)
target_link_libraries(deaftly_ppocr PRIVATE ncnn android log)
