package com.ravendarque.deaftly.core.ocr

public data class RecognitionResult(
    val blocks: List<TextBlock>,
) {
    val text: String get() = blocks.joinToString("\n") { it.text }

    public companion object {
        public val EMPTY: RecognitionResult = RecognitionResult(emptyList())
    }
}

public data class TextBlock(
    val lines: List<TextLine>,
    val boundingBox: BoundingBox,
) {
    val text: String get() = lines.joinToString("\n") { it.text }
}

/**
 * [corners] and [angleDegrees] are set when the engine reports a rotated line, as
 * PP-OCR does with its quadrilaterals; [angleDegrees] is the clockwise slope of the
 * baseline. [confidence] is in 0..1 where the engine provides it.
 */
public data class TextLine(
    val text: String,
    val boundingBox: BoundingBox,
    val words: List<TextWord> = emptyList(),
    val corners: Quad? = null,
    val angleDegrees: Float? = null,
    val confidence: Float? = null,
)

/**
 * [corners] is the word's quadrilateral in the same order as [TextLine.corners]: top-left,
 * top-right, bottom-right, bottom-left of the word as read, so it follows rotated text.
 */
public data class TextWord(
    val text: String,
    val boundingBox: BoundingBox,
    val confidence: Float? = null,
    val corners: Quad? = null,
)
