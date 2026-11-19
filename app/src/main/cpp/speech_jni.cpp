#include <android/asset_manager_jni.h>
#include <jni.h>

#include <memory>
#include <mutex>
#include <set>
#include <string>
#include <vector>

#include "jni_strings.h"
#include "sherpa-ncnn/csrc/recognizer.h"

namespace {

constexpr jint kSampleRate = 16000;
constexpr jint kMaxSamples = 10 * kSampleRate;
constexpr jint kNoChange = 0;
constexpr jint kPartial = 1;
constexpr jint kEndpoint = 2;

struct Engine {
    std::unique_ptr<sherpa_ncnn::Recognizer> recognizer;
    std::unique_ptr<sherpa_ncnn::Stream> stream;
    std::string last;
    std::string current;
};

std::mutex registryLock;
std::set<Engine*> registry;

Engine* lookup(jlong handle) {
    auto* engine = reinterpret_cast<Engine*>(handle);
    std::lock_guard<std::mutex> guard(registryLock);
    return registry.count(engine) ? engine : nullptr;
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_ravendarque_deaftly_speech_NativeSherpa_create(
    JNIEnv* env, jobject, jobject assetManager, jstring dir, jint threads, jfloat trailingSilence) {
    if (assetManager == nullptr || dir == nullptr || threads < 1 || threads > 8 || !(trailingSilence > 0.0f)) return 0;
    AAssetManager* assets = AAssetManager_fromJava(env, assetManager);
    if (assets == nullptr) return 0;
    const std::string root = toUtf8(env, dir) + "/";

    sherpa_ncnn::RecognizerConfig config;
    config.feat_config.sampling_rate = kSampleRate;
    config.feat_config.feature_dim = 80;
    config.model_config.encoder_param = root + "encoder.ncnn.param";
    config.model_config.encoder_bin = root + "encoder.ncnn.bin";
    config.model_config.decoder_param = root + "decoder.ncnn.param";
    config.model_config.decoder_bin = root + "decoder.ncnn.bin";
    config.model_config.joiner_param = root + "joiner.ncnn.param";
    config.model_config.joiner_bin = root + "joiner.ncnn.bin";
    config.model_config.tokens = root + "tokens.txt";
    config.model_config.use_vulkan_compute = false;
    config.model_config.encoder_opt.num_threads = threads;
    config.model_config.decoder_opt.num_threads = threads;
    config.model_config.joiner_opt.num_threads = threads;
    config.decoder_config.method = "greedy_search";
    config.enable_endpoint = true;
    config.endpoint_config.rule1 = sherpa_ncnn::EndpointRule(false, 2.4f, 0.0f);
    config.endpoint_config.rule2 = sherpa_ncnn::EndpointRule(true, trailingSilence, 0.0f);
    config.endpoint_config.rule3 = sherpa_ncnn::EndpointRule(false, 0.0f, 20.0f);

    auto engine = std::make_unique<Engine>();
    engine->recognizer = std::make_unique<sherpa_ncnn::Recognizer>(assets, config);
    engine->stream = engine->recognizer->CreateStream();
    Engine* raw = engine.release();
    std::lock_guard<std::mutex> guard(registryLock);
    registry.insert(raw);
    return reinterpret_cast<jlong>(raw);
}

JNIEXPORT jint JNICALL Java_com_ravendarque_deaftly_speech_NativeSherpa_accept(
    JNIEnv* env, jobject, jlong handle, jfloatArray samples) {
    Engine* engine = lookup(handle);
    if (engine == nullptr || samples == nullptr) return kNoChange;
    const jsize count = env->GetArrayLength(samples);
    if (count <= 0 || count > kMaxSamples) return kNoChange;
    std::vector<float> buffer((size_t)count);
    env->GetFloatArrayRegion(samples, 0, count, buffer.data());
    if (env->ExceptionCheck()) return kNoChange;

    engine->stream->AcceptWaveform(kSampleRate, buffer.data(), (int32_t)count);
    while (engine->recognizer->IsReady(engine->stream.get())) {
        engine->recognizer->DecodeStream(engine->stream.get());
    }
    const bool endpoint = engine->recognizer->IsEndpoint(engine->stream.get());
    engine->current = engine->recognizer->GetResult(engine->stream.get()).text;
    if (endpoint) {
        engine->recognizer->Reset(engine->stream.get());
        engine->last.clear();
        return kEndpoint;
    }
    if (engine->current == engine->last) return kNoChange;
    engine->last = engine->current;
    return kPartial;
}

JNIEXPORT jstring JNICALL Java_com_ravendarque_deaftly_speech_NativeSherpa_text(JNIEnv* env, jobject, jlong handle) {
    Engine* engine = lookup(handle);
    return toJString(env, engine == nullptr ? std::string() : engine->current);
}

JNIEXPORT void JNICALL Java_com_ravendarque_deaftly_speech_NativeSherpa_release(JNIEnv*, jobject, jlong handle) {
    Engine* engine;
    {
        std::lock_guard<std::mutex> guard(registryLock);
        auto found = registry.find(reinterpret_cast<Engine*>(handle));
        if (found == registry.end()) return;
        engine = *found;
        registry.erase(found);
    }
    delete engine;
}

}  // extern "C"
