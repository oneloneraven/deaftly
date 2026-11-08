package com.ravendarque.deaftly.core.ocr

public class FakeTextRecogniser(
    private val script: List<RecognitionResult>,
) : TextRecogniser {
    public constructor(result: RecognitionResult) : this(listOf(result))

    private val received = mutableListOf<RecognitionFrame>()

    public val frames: List<RecognitionFrame> get() = received.toList()
    public var isClosed: Boolean = false
        private set

    init {
        require(script.isNotEmpty()) { "Script must hold at least one result" }
    }

    override suspend fun recognise(frame: RecognitionFrame): RecognitionResult {
        check(!isClosed) { "Recogniser is closed" }
        received += frame
        return script[minOf(received.size, script.size) - 1]
    }

    override fun close() {
        isClosed = true
    }
}
