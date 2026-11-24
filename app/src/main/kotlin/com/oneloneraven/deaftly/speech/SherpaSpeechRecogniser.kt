package com.oneloneraven.deaftly.speech

import android.content.Context
import android.content.res.AssetManager
import com.oneloneraven.deaftly.core.speech.SpeechEvent
import com.oneloneraven.deaftly.core.speech.SpeechRecogniser
import java.io.IOException

internal const val PAUSE_SECONDS = 2.0f

internal data class SherpaConfig(
    val threads: Int = 2,
    val trailingSilenceSeconds: Float = PAUSE_SECONDS,
    val modelDir: String = MODEL_DIR,
)

internal const val MODEL_DIR = "speech"

private const val ENCODER_PARAM_BYTES = 161_888L
private const val ENCODER_BIN_BYTES = 139_191_256L
private const val DECODER_PARAM_BYTES = 437L
private const val DECODER_BIN_BYTES = 520_200L
private const val JOINER_PARAM_BYTES = 488L
private const val JOINER_BIN_BYTES = 1_435_612L
private const val TOKENS_BYTES = 5_048L

private val MODEL_FILE_BYTES =
    mapOf(
        "encoder.ncnn.param" to ENCODER_PARAM_BYTES,
        "encoder.ncnn.bin" to ENCODER_BIN_BYTES,
        "decoder.ncnn.param" to DECODER_PARAM_BYTES,
        "decoder.ncnn.bin" to DECODER_BIN_BYTES,
        "joiner.ncnn.param" to JOINER_PARAM_BYTES,
        "joiner.ncnn.bin" to JOINER_BIN_BYTES,
        "tokens.txt" to TOKENS_BYTES,
    )

internal class SherpaSpeechRecogniser(
    context: Context,
    config: SherpaConfig = SherpaConfig(),
) : SpeechRecogniser {
    private val native = NativeSherpa()
    private var handle: Long

    init {
        val assets = context.applicationContext.assets
        checkModel(context, config.modelDir)
        handle = native.create(assets, config.modelDir, config.threads, config.trailingSilenceSeconds)
        check(handle != 0L) { "The speech engine could not be created" }
    }

    @Synchronized
    override fun accept(samples: FloatArray): SpeechEvent? {
        check(handle != 0L) { "The speech engine is closed" }
        val outcome = native.accept(handle, samples)
        if (outcome == NativeSherpa.NO_CHANGE) return null
        val text = toSentenceCase(native.text(handle))
        return when {
            outcome == NativeSherpa.ENDPOINT -> text.takeIf(String::isNotEmpty)?.let(SpeechEvent::Utterance)
            else -> SpeechEvent.Partial(text)
        }
    }

    @Synchronized
    override fun close() {
        if (handle == 0L) return
        native.release(handle)
        handle = 0L
    }

    private fun checkModel(
        context: Context,
        dir: String,
    ) {
        MODEL_FILE_BYTES.forEach { (name, bytes) ->
            val actual =
                try {
                    context.assets.open("$dir/$name", AssetManager.ACCESS_STREAMING).use { it.available().toLong() }
                } catch (_: IOException) {
                    -1L
                }
            check(actual == bytes) { "The speech model file $name is missing or the wrong size" }
        }
    }
}

internal fun toSentenceCase(raw: String): String {
    val lower = raw.trim().lowercase()
    if (lower.isEmpty()) return lower
    val words = lower.split(' ').map { if (it == "i" || it.startsWith("i'")) it.replaceFirstChar(Char::uppercaseChar) else it }
    return words.joinToString(" ").replaceFirstChar { it.uppercaseChar() }
}
