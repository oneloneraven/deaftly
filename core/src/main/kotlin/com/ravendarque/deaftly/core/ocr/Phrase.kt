package com.ravendarque.deaftly.core.ocr

public data class PhraseWord(
    val text: String,
    val quad: Quad,
)

/**
 * A line of recognised text. [quad] and each word's quad are in viewport units: x and y run from
 * 0 to 1 across the part of the frame the user sees.
 */
public data class Phrase(
    val id: Long,
    val text: String,
    val quad: Quad,
    val words: List<PhraseWord>,
)

public class DetectedPhrase(
    public val text: String,
    public val quad: Quad,
    public val words: List<PhraseWord>,
)
