package com.ravendarque.deaftly.core.ocr

/**
 * Tells real text from the shapes and marks a detector outlines by mistake: a line is dropped when it
 * is blank, when fewer than [MIN_TEXT_RATIO] of its non-space characters are letters or digits, or
 * when the engine's confidence in it is below the minimum. A line without a confidence is kept.
 */
public object TextLineFilter {
    public const val DEFAULT_MIN_CONFIDENCE: Float = 0.5f
    public const val MIN_TEXT_RATIO: Float = 0.5f

    public fun isText(
        line: TextLine,
        minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
    ): Boolean {
        val characters = line.text.filterNot(Char::isWhitespace)
        val textual = characters.isNotEmpty() && characters.count(Char::isLetterOrDigit) >= characters.length * MIN_TEXT_RATIO
        return textual && (line.confidence?.let { it >= minConfidence } ?: true)
    }

    public fun filter(
        result: RecognitionResult,
        minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
    ): RecognitionResult =
        RecognitionResult(
            result.blocks.mapNotNull { block ->
                val lines = block.lines.filter { isText(it, minConfidence) }
                if (lines.isEmpty()) null else TextBlock(lines, block.boundingBox)
            },
        )
}
