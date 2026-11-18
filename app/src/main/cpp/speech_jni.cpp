#include <jni.h>

#include "sherpa-ncnn/csrc/recognizer.h"

extern "C" JNIEXPORT jint JNICALL Java_com_ravendarque_deaftly_speech_NativeSherpa_probe(JNIEnv *, jclass) {
    sherpa_ncnn::RecognizerConfig config;
    config.enable_endpoint = true;
    sherpa_ncnn::Recognizer recognizer(config);
    return 1;
}
