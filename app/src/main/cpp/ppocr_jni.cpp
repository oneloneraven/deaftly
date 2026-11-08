#include <android/asset_manager_jni.h>
#include <jni.h>

#include <memory>
#include <string>
#include <vector>

#include "ppocr_engine.h"

namespace {

constexpr jint kInvalid = -1;
constexpr jint kMaxSide = 16384;
constexpr int64_t kMaxPixels = 40'000'000;

std::string toUtf8(JNIEnv* env, jstring value) {
    const jsize length = env->GetStringLength(value);
    const jchar* chars = env->GetStringChars(value, nullptr);
    std::string out;
    for (jsize i = 0; i < length; i++) {
        uint32_t cp = chars[i];
        if (cp >= 0xD800 && cp < 0xDC00 && i + 1 < length) {
            cp = 0x10000 + ((cp - 0xD800) << 10) + (chars[++i] - 0xDC00);
        }
        if (cp < 0x80) {
            out += (char)cp;
        } else if (cp < 0x800) {
            out += (char)(0xC0 | (cp >> 6));
            out += (char)(0x80 | (cp & 0x3F));
        } else if (cp < 0x10000) {
            out += (char)(0xE0 | (cp >> 12));
            out += (char)(0x80 | ((cp >> 6) & 0x3F));
            out += (char)(0x80 | (cp & 0x3F));
        } else {
            out += (char)(0xF0 | (cp >> 18));
            out += (char)(0x80 | ((cp >> 12) & 0x3F));
            out += (char)(0x80 | ((cp >> 6) & 0x3F));
            out += (char)(0x80 | (cp & 0x3F));
        }
    }
    env->ReleaseStringChars(value, chars);
    return out;
}

jstring toJString(JNIEnv* env, const std::string& utf8) {
    std::vector<jchar> units;
    units.reserve(utf8.size());
    size_t i = 0;
    while (i < utf8.size()) {
        const uint8_t b = (uint8_t)utf8[i];
        uint32_t cp;
        int extra;
        if (b < 0x80) {
            cp = b;
            extra = 0;
        } else if ((b >> 5) == 0x6) {
            cp = b & 0x1F;
            extra = 1;
        } else if ((b >> 4) == 0xE) {
            cp = b & 0x0F;
            extra = 2;
        } else {
            cp = b & 0x07;
            extra = 3;
        }
        i++;
        for (int k = 0; k < extra && i < utf8.size(); k++, i++) cp = (cp << 6) | ((uint8_t)utf8[i] & 0x3F);
        if (cp >= 0x10000) {
            cp -= 0x10000;
            units.push_back((jchar)(0xD800 + (cp >> 10)));
            units.push_back((jchar)(0xDC00 + (cp & 0x3FF)));
        } else {
            units.push_back((jchar)cp);
        }
    }
    return env->NewString(units.data(), (jsize)units.size());
}

ppocr::Engine* engine(jlong handle) { return reinterpret_cast<ppocr::Engine*>(handle); }

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_ravendarque_deaftly_ocr_paddle_NativePaddleOcr_create(
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

JNIEXPORT void JNICALL Java_com_ravendarque_deaftly_ocr_paddle_NativePaddleOcr_release(JNIEnv*, jobject, jlong handle) {
    delete engine(handle);
}

JNIEXPORT jint JNICALL Java_com_ravendarque_deaftly_ocr_paddle_NativePaddleOcr_recognise(
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

JNIEXPORT jfloatArray JNICALL Java_com_ravendarque_deaftly_ocr_paddle_NativePaddleOcr_geometry(JNIEnv* env, jobject, jlong handle) {
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

JNIEXPORT jobjectArray JNICALL Java_com_ravendarque_deaftly_ocr_paddle_NativePaddleOcr_texts(JNIEnv* env, jobject, jlong handle) {
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

JNIEXPORT jdoubleArray JNICALL Java_com_ravendarque_deaftly_ocr_paddle_NativePaddleOcr_timings(JNIEnv* env, jobject, jlong handle) {
    const ppocr::Timings& t = engine(handle)->timings();
    const double values[4] = {t.prepareMs, t.detectMs, t.postprocessMs, t.recogniseMs};
    jdoubleArray result = env->NewDoubleArray(4);
    env->SetDoubleArrayRegion(result, 0, 4, values);
    return result;
}

}  // extern "C"
