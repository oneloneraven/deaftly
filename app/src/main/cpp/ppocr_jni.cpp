#include <android/asset_manager_jni.h>
#include <jni.h>

#include <memory>
#include <string>
#include <vector>

#include "jni_strings.h"
#include "ppocr_engine.h"

namespace {

constexpr jint kInvalid = -1;
constexpr jint kMaxSide = 16384;
constexpr int64_t kMaxPixels = 40'000'000;

ppocr::Engine* engine(jlong handle) { return reinterpret_cast<ppocr::Engine*>(handle); }

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_oneloneraven_deaftly_ocr_paddle_NativePaddleOcr_create(
    JNIEnv* env, jobject, jobject assetManager, jstring detParam, jstring detBin, jstring recParam, jstring recBin,
    jstring dict, jint detSide, jint threads, jboolean fp16) {
    ppocr::Config config;
    config.detParam = toUtf8(env, detParam);
    config.detBin = toUtf8(env, detBin);
    config.recParam = toUtf8(env, recParam);
    config.recBin = toUtf8(env, recBin);
    config.dictPath = toUtf8(env, dict);
    config.detSide = detSide;
    config.threads = threads;
    config.fp16 = fp16;
    auto created = std::make_unique<ppocr::Engine>();
    if (!created->load(AAssetManager_fromJava(env, assetManager), config)) return 0;
    return reinterpret_cast<jlong>(created.release());
}

JNIEXPORT void JNICALL Java_com_oneloneraven_deaftly_ocr_paddle_NativePaddleOcr_release(JNIEnv*, jobject, jlong handle) {
    delete engine(handle);
}

JNIEXPORT jint JNICALL Java_com_oneloneraven_deaftly_ocr_paddle_NativePaddleOcr_recognise(
    JNIEnv* env, jobject, jlong handle, jobject pixels, jint width, jint height, jint strideBytes, jint rotation) {
    if (handle == 0 || pixels == nullptr) return kInvalid;
    const auto* data = static_cast<const uint8_t*>(env->GetDirectBufferAddress(pixels));
    if (!data) return kInvalid;
    const int64_t capacity = env->GetDirectBufferCapacity(pixels);
    const bool validRotation = rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270;
    if (!validRotation || width <= 0 || height <= 0 || width > kMaxSide || height > kMaxSide) return kInvalid;
    if ((int64_t)width * height > kMaxPixels) return kInvalid;
    const int64_t rowBytes = (int64_t)width * 4;
    if (strideBytes < rowBytes || capacity < (int64_t)(height - 1) * strideBytes + rowBytes) return kInvalid;
    engine(handle)->run(data, width, height, strideBytes, rotation);
    return (jint)engine(handle)->lines().size();
}

JNIEXPORT jfloatArray JNICALL Java_com_oneloneraven_deaftly_ocr_paddle_NativePaddleOcr_geometry(JNIEnv* env, jobject, jlong handle) {
    std::vector<float> flat;
    for (const ppocr::Line& line : engine(handle)->lines()) {
        flat.push_back(line.confidence);
        flat.insert(flat.end(), line.quad, line.quad + 8);
        flat.push_back((float)line.words.size());
        for (const ppocr::Word& word : line.words) {
            flat.push_back(word.confidence);
            flat.insert(flat.end(), word.quad, word.quad + 8);
        }
    }
    jfloatArray result = env->NewFloatArray((jsize)flat.size());
    env->SetFloatArrayRegion(result, 0, (jsize)flat.size(), flat.data());
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_oneloneraven_deaftly_ocr_paddle_NativePaddleOcr_texts(JNIEnv* env, jobject, jlong handle) {
    std::vector<const std::string*> texts;
    for (const ppocr::Line& line : engine(handle)->lines()) {
        texts.push_back(&line.text);
        for (const ppocr::Word& word : line.words) texts.push_back(&word.text);
    }
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray((jsize)texts.size(), stringClass, nullptr);
    for (size_t i = 0; i < texts.size(); i++) {
        jstring s = toJString(env, *texts[i]);
        env->SetObjectArrayElement(result, (jsize)i, s);
        env->DeleteLocalRef(s);
    }
    return result;
}

JNIEXPORT jdoubleArray JNICALL Java_com_oneloneraven_deaftly_ocr_paddle_NativePaddleOcr_timings(JNIEnv* env, jobject, jlong handle) {
    const ppocr::Timings& t = engine(handle)->timings();
    const double values[4] = {t.prepareMs, t.detectMs, t.postprocessMs, t.recogniseMs};
    jdoubleArray result = env->NewDoubleArray(4);
    env->SetDoubleArrayRegion(result, 0, 4, values);
    return result;
}

}  // extern "C"
