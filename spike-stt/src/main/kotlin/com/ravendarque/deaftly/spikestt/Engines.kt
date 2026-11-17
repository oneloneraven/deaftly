package com.ravendarque.deaftly.spikestt

import com.k2fsa.sherpa.ncnn.DecoderConfig
import com.k2fsa.sherpa.ncnn.FeatureExtractorConfig
import com.k2fsa.sherpa.ncnn.ModelConfig
import com.k2fsa.sherpa.ncnn.RecognizerConfig
import com.k2fsa.sherpa.ncnn.SherpaNcnn
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class Transcript(val text: String, val firstPartialChunk: Int?, val finaliseMs: Double, val chunkMs: List<Double>)

interface Engine {
    val name: String
    fun transcribe(samples: FloatArray): Transcript
}

const val CHUNK = 1600

class SherpaEngine(override val name: String, dir: File, method: String, threads: Int) : Engine {
    private val recogniser: SherpaNcnn

    init {
        fun f(n: String) = File(dir, n).absolutePath
        recogniser =
            SherpaNcnn(
                RecognizerConfig(
                    featConfig = FeatureExtractorConfig(16000f, 80),
                    modelConfig =
                        ModelConfig(
                            encoderParam = f("encoder_jit_trace-pnnx.ncnn.param"), encoderBin = f("encoder_jit_trace-pnnx.ncnn.bin"),
                            decoderParam = f("decoder_jit_trace-pnnx.ncnn.param"), decoderBin = f("decoder_jit_trace-pnnx.ncnn.bin"),
                            joinerParam = f("joiner_jit_trace-pnnx.ncnn.param"), joinerBin = f("joiner_jit_trace-pnnx.ncnn.bin"),
                            tokens = f("tokens.txt"), numThreads = threads, useGPU = false,
                        ),
                    decoderConfig = DecoderConfig(method = method, numActivePaths = 4),
                    enableEndpoint = false,
                ),
            )
    }

    override fun transcribe(samples: FloatArray): Transcript {
        recogniser.reset(true)
        var first: Int? = null
        val times = ArrayList<Double>()
        var i = 0
        while (i < samples.size) {
            val t0 = System.nanoTime()
            recogniser.acceptSamples(samples.copyOfRange(i, minOf(i + CHUNK, samples.size)))
            while (recogniser.isReady()) recogniser.decode()
            times += (System.nanoTime() - t0) / 1e6
            if (first == null && recogniser.text.isNotBlank()) first = i / CHUNK
            i += CHUNK
        }
        val t0 = System.nanoTime()
        recogniser.acceptSamples(FloatArray(8000))
        recogniser.inputFinished()
        while (recogniser.isReady()) recogniser.decode()
        val text = recogniser.text
        return Transcript(text, first, (System.nanoTime() - t0) / 1e6, times)
    }
}

class VoskEngine(override val name: String, dir: File) : Engine {
    private val model = Model(dir.absolutePath)

    override fun transcribe(samples: FloatArray): Transcript {
        Recognizer(model, 16000f).use { rec ->
            var first: Int? = null
            val times = ArrayList<Double>()
            val parts = ArrayList<String>()
            var i = 0
            while (i < samples.size) {
                val chunk = samples.copyOfRange(i, minOf(i + CHUNK, samples.size))
                val pcm = ShortArray(chunk.size) { (chunk[it].coerceIn(-1f, 1f) * 32767).toInt().toShort() }
                val t0 = System.nanoTime()
                val done = rec.acceptWaveForm(pcm, pcm.size)
                val partial = if (done) "" else JSONObject(rec.partialResult).optString("partial")
                if (done) parts += JSONObject(rec.result).optString("text")
                times += (System.nanoTime() - t0) / 1e6
                if (first == null && partial.isNotBlank()) first = i / CHUNK
                i += CHUNK
            }
            val t0 = System.nanoTime()
            parts += JSONObject(rec.finalResult).optString("text")
            return Transcript(parts.filter { it.isNotBlank() }.joinToString(" "), first, (System.nanoTime() - t0) / 1e6, times)
        }
    }
}
