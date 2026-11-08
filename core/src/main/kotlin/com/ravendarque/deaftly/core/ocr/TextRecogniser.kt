package com.ravendarque.deaftly.core.ocr

public interface TextRecogniser : AutoCloseable {
    public suspend fun recognise(frame: RecognitionFrame): RecognitionResult

    override fun close()
}
