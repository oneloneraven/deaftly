cmake_minimum_required(VERSION 3.22)
project(deaftly_native LANGUAGES C CXX)

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

foreach(var SHERPA_NCNN_SRC KALDI_FBANK_SRC KISSFFT_SRC)
    if(NOT DEFINED ${var})
        message(FATAL_ERROR "${var} must point at the extracted source tree fetched by Gradle")
    endif()
endforeach()

set(sherpa_csrc "${SHERPA_NCNN_SRC}/sherpa-ncnn/csrc")
set(sherpa_srcs
    context-graph.cc
    conv-emformer-model.cc
    decoder.cc
    endpoint.cc
    features.cc
    file-utils.cc
    greedy-search-decoder.cc
    hypothesis.cc
    lstm-model.cc
    math.cc
    meta-data.cc
    model.cc
    modified-beam-search-decoder.cc
    poolingmodulenoproj.cc
    recognizer.cc
    resample.cc
    simpleupsample.cc
    stack.cc
    stream.cc
    symbol-table.cc
    tensorasstrided.cc
    text-utils.cc
    zipformer-model.cc)
list(TRANSFORM sherpa_srcs PREPEND "${sherpa_csrc}/")

set(fbank_csrc "${KALDI_FBANK_SRC}/kaldi-native-fbank/csrc")
set(fbank_srcs
    feature-fbank.cc
    feature-functions.cc
    feature-mfcc.cc
    feature-window.cc
    istft.cc
    kaldi-math.cc
    mel-computations.cc
    online-feature.cc
    rfft.cc
    stft.cc
    whisper-feature.cc)
list(TRANSFORM fbank_srcs PREPEND "${fbank_csrc}/")

add_library(deaftly_speech SHARED speech_jni.cpp ${sherpa_srcs} ${fbank_srcs}
    "${KISSFFT_SRC}/kiss_fft.c" "${KISSFFT_SRC}/kiss_fftr.c")
target_include_directories(deaftly_speech PRIVATE "${SHERPA_NCNN_SRC}" "${KALDI_FBANK_SRC}" "${KISSFFT_SRC}")
target_compile_definitions(deaftly_speech PRIVATE kiss_fft_scalar=float)
target_compile_options(deaftly_speech PRIVATE -O3 -fvisibility=hidden)
target_link_options(deaftly_speech PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL -Wl,-z,max-page-size=16384)
target_link_libraries(deaftly_speech PRIVATE ncnn android log)
