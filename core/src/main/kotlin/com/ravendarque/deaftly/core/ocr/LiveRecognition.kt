package com.ravendarque.deaftly.core.ocr

/**
 * Turns camera frames into steady phrases in viewport units. Not thread safe: feed it one frame at
 * a time, as a single analysis thread does.
 */
public class LiveRecognition(
    private val recogniser: TextRecogniser,
    private val tracker: PhraseTracker = PhraseTracker(),
) {
    /** [crop] is the visible region of the frame as stored, such as CameraX's `cropRect`. */
    public suspend fun process(
        frame: RecognitionFrame,
        crop: BoundingBox? = null,
    ): List<Phrase> {
        val mapping = ViewportMapping.of(frame, crop)
        return tracker.update(detect(recogniser.recognise(frame), mapping))
    }

    public fun detect(
        result: RecognitionResult,
        mapping: ViewportMapping,
    ): List<DetectedPhrase> =
        result.blocks
            .flatMap { it.lines }
            .flatMap(LineSegmenter::split)
            .filter { TextLineFilter.isText(it) }
            .map { line ->
                val quad = mapping.toViewport(line.corners ?: line.boundingBox.toQuad())
                val words =
                    line.words
                        .filter { it.text.isNotBlank() }
                        .map { PhraseWord(it.text, mapping.toViewport(it.corners ?: it.boundingBox.toQuad())) }
                        .ifEmpty { listOf(PhraseWord(line.text, quad)) }
                DetectedPhrase(line.text, quad, words)
            }
}
