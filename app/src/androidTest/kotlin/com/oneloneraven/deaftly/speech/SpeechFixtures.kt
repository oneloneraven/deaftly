package com.oneloneraven.deaftly.speech

import android.content.Context
import com.oneloneraven.deaftly.core.speech.SPEECH_CHUNK_SAMPLES
import com.oneloneraven.deaftly.core.speech.SpeechEvent
import com.oneloneraven.deaftly.core.speech.SpeechRecogniser
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object SpeechFixtures {
    const val ID = "1089-134686-0008"

    fun samples(context: Context): FloatArray = parseWav(context.assets.open("speech-fixtures/$ID.wav").use { it.readBytes() })

    fun transcript(context: Context): String = context.assets.open("speech-fixtures/$ID.txt").use { String(it.readBytes()).trim() }

    fun feed(
        recogniser: SpeechRecogniser,
        samples: FloatArray,
    ): List<SpeechEvent> =
        samples
            .toList()
            .chunked(SPEECH_CHUNK_SAMPLES)
            .mapNotNull { recogniser.accept(it.toFloatArray()) }

    fun wordErrorRate(
        reference: String,
        hypothesis: String,
    ): Double {
        val ref = words(reference)
        val hyp = words(hypothesis)
        var previous = IntArray(hyp.size + 1) { it }
        for (i in 1..ref.size) {
            val row = IntArray(hyp.size + 1)
            row[0] = i
            for (j in 1..hyp.size) {
                val substitution = previous[j - 1] + if (ref[i - 1] == hyp[j - 1]) 0 else 1
                row[j] = minOf(substitution, previous[j] + 1, row[j - 1] + 1)
            }
            previous = row
        }
        return previous[hyp.size].toDouble() / ref.size
    }

    private fun words(text: String): List<String> =
        text
            .lowercase()
            .replace(Regex("[^a-z0-9' ]"), " ")
            .split(Regex("\\s+"))
            .filter(String::isNotEmpty)

    private fun parseWav(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = RIFF_HEADER_BYTES
        while (offset + CHUNK_HEADER_BYTES <= bytes.size) {
            val id = String(bytes, offset, ID_BYTES, Charsets.US_ASCII)
            val size = buffer.getInt(offset + ID_BYTES)
            if (id == "data") {
                val start = offset + CHUNK_HEADER_BYTES
                return FloatArray(minOf(size, bytes.size - start) / 2) { buffer.getShort(start + it * 2) / PCM_SCALE }
            }
            offset += CHUNK_HEADER_BYTES + size
        }
        error("No data chunk in the fixture")
    }

    private const val RIFF_HEADER_BYTES = 12
    private const val CHUNK_HEADER_BYTES = 8
    private const val ID_BYTES = 4
    private const val PCM_SCALE = 32_768f
}
