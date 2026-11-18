package com.ravendarque.deaftly.spikestt

import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class SttBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val args = InstrumentationRegistry.getArguments()
    private val root = context.filesDir

    private fun wav(file: File): FloatArray {
        val bytes = file.readBytes()
        var p = 12
        while (p < bytes.size - 8) {
            val id = String(bytes, p, 4)
            val size = ByteBuffer.wrap(bytes, p + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (id == "data") {
                val sb = ByteBuffer.wrap(bytes, p + 8, size).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                return FloatArray(sb.remaining()) { sb.get(it) / 32768f }
            }
            p += 8 + size
        }
        error("no data chunk in $file")
    }

    private fun engine(name: String): Engine =
        when (name) {
            "sherpa-zipformer-en" -> SherpaEngine(name, File(root, "models/sherpa-ncnn-streaming-zipformer-en-2023-02-13"), "greedy_search", 2)
            "sherpa-zipformer-en-beam" -> SherpaEngine(name, File(root, "models/sherpa-ncnn-streaming-zipformer-en-2023-02-13"), "modified_beam_search", 2)
            "sherpa-zipformer-20M" -> SherpaEngine(name, File(root, "models/sherpa-ncnn-streaming-zipformer-20M-2023-02-17"), "greedy_search", 2)
            "sherpa-20M-int8" -> SherpaEngine(name, File(root, "models/sherpa-ncnn-streaming-zipformer-20M-2023-02-17-int8"), "greedy_search", 2)
            "sherpa-librigiga-70M" -> SherpaEngine(name, File(root, "models/sherpa-ncnn-zipformer-libri-giga-70M-2023-04-04"), "greedy_search", 2)
            "sherpa-librigiga-70M-int8" -> SherpaEngine(name, File(root, "models/sherpa-ncnn-zipformer-libri-giga-70M-2023-04-04-int8"), "greedy_search", 2)
            "vosk-small-en-us" -> VoskEngine(name, File(root, "models/vosk-model-small-en-us-0.15"))
            "vosk-en-us-lgraph" -> VoskEngine(name, File(root, "models/vosk-model-en-us-0.22-lgraph"))
            else -> AndroidOnDeviceEngine(context, name.removePrefix("android-"))
        }

    @Test
    fun run() {
        val name = args.getString("engine")!!
        val conditions = args.getString("conditions")!!.split(",")
        val manifest = JSONArray(File(root, "clips/manifest.json").readText())
        val loadStart = System.nanoTime()
        val engine = engine(name)
        val loadMs = (System.nanoTime() - loadStart) / 1e6
        val out = JSONArray()
        for (cond in conditions) {
            for (i in 0 until minOf(manifest.length(), args.getString("limit")?.toInt() ?: Int.MAX_VALUE)) {
                val id = manifest.getJSONObject(i).getString("id")
                val samples = wav(File(root, "clips/$cond/$id.wav"))
                val t = engine.transcribe(samples)
                val mem = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
                out.put(
                    JSONObject()
                        .put("engine", name).put("cond", cond).put("id", id).put("hyp", t.text)
                        .put("first_partial_chunk", t.firstPartialChunk ?: JSONObject.NULL)
                        .put("finalise_ms", t.finaliseMs).put("chunk_ms", JSONArray(t.chunkMs))
                        .put("audio_s", samples.size / 16000.0).put("pss_kb", mem.totalPss),
                )
            }
            File(root, "results").mkdirs()
            File(root, "results/device-$name.json").writeText(JSONObject().put("load_ms", loadMs).put("rows", out).toString())
        }
        File(root, "results").mkdirs()
        File(root, "results/device-$name.json").writeText(JSONObject().put("load_ms", loadMs).put("rows", out).toString())
    }

    @Test
    fun languages() {
        val lang = args.getString("lang") ?: return
        val done = java.util.concurrent.CountDownLatch(1)
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, lang)
        main.post {
            val r = android.speech.SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            r.checkRecognitionSupport(intent, context.mainExecutor, object : android.speech.RecognitionSupportCallback {
                override fun onSupportResult(s: android.speech.RecognitionSupport) {
                    android.util.Log.d("SPIKESTT", "installed=${s.installedOnDeviceLanguages} supported=${s.supportedOnDeviceLanguages} pending=${s.pendingOnDeviceLanguages}")
                    if (args.getString("download") != null) r.triggerModelDownload(intent)
                    done.countDown()
                }
                override fun onError(error: Int) { android.util.Log.d("SPIKESTT", "support error $error"); done.countDown() }
            })
        }
        done.await(30, java.util.concurrent.TimeUnit.SECONDS)
    }
}
