package com.oneloneraven.deaftly.core.speech

public const val SPEECH_SAMPLE_RATE: Int = 16_000
public const val SPEECH_CHUNK_SAMPLES: Int = 1_600

public sealed interface SpeechEvent {
    public data class Partial(
        val text: String,
    ) : SpeechEvent

    public data class Utterance(
        val text: String,
    ) : SpeechEvent
}

public interface SpeechRecogniser : AutoCloseable {
    public fun accept(samples: FloatArray): SpeechEvent?

    override fun close()
}
