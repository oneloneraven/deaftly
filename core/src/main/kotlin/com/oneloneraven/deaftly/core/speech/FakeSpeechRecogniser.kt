package com.oneloneraven.deaftly.core.speech

public class FakeSpeechRecogniser(
    private val script: List<SpeechEvent?>,
) : SpeechRecogniser {
    public var accepted: Int = 0
        private set
    public var isClosed: Boolean = false
        private set

    override fun accept(samples: FloatArray): SpeechEvent? {
        check(!isClosed) { "Recogniser is closed" }
        return script.getOrNull(accepted++)
    }

    override fun close() {
        isClosed = true
    }
}
