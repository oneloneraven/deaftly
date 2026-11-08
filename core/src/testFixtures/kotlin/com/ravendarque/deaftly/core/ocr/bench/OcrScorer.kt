package com.ravendarque.deaftly.core.ocr.bench

import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextLine
import com.ravendarque.deaftly.core.ocr.TextWord

public data class TextErrors(
    val charCaseSensitive: ErrorCount,
    val charCaseInsensitive: ErrorCount,
    val wordCaseSensitive: ErrorCount,
    val wordCaseInsensitive: ErrorCount,
) {
    public operator fun plus(other: TextErrors): TextErrors =
        TextErrors(
            charCaseSensitive + other.charCaseSensitive,
            charCaseInsensitive + other.charCaseInsensitive,
            wordCaseSensitive + other.wordCaseSensitive,
            wordCaseInsensitive + other.wordCaseInsensitive,
        )

    public companion object {
        public val ZERO: TextErrors = TextErrors(ErrorCount.ZERO, ErrorCount.ZERO, ErrorCount.ZERO, ErrorCount.ZERO)

        public fun ofBag(
            reference: String,
            hypothesis: String,
        ): TextErrors {
            val sensitive = TextMetrics.bagErrors(reference, hypothesis, ignoreCase = false)
            val insensitive = TextMetrics.bagErrors(reference, hypothesis, ignoreCase = true)
            return TextErrors(sensitive.first, insensitive.first, sensitive.second, insensitive.second)
        }

        public fun of(
            reference: String,
            hypothesis: String,
        ): TextErrors =
            TextErrors(
                TextMetrics.characterErrors(reference, hypothesis, ignoreCase = false),
                TextMetrics.characterErrors(reference, hypothesis, ignoreCase = true),
                TextMetrics.wordErrors(reference, hypothesis, ignoreCase = false),
                TextMetrics.wordErrors(reference, hypothesis, ignoreCase = true),
            )
    }
}

public data class ImageScore(
    val id: String,
    val category: String,
    val tags: List<String>,
    val lighting: String,
    val truthLines: Int,
    val predictedLines: Int,
    val matchedLines: Int,
    val truthWords: Int,
    val wordBoxHits: Int,
    val wordBoxAndTextHits: Int,
    val ordered: TextErrors,
    val bag: TextErrors,
    val predictedText: String,
    val orderPreserved: Boolean = true,
    val xyCutOrderPreserved: Boolean = true,
    val nativeOrderPreserved: Boolean = true,
)

public enum class OrderMode { XY_CUT, NATIVE }

public object OcrScorer {
    public const val LINE_IOU: Double = 0.5
    public const val WORD_IOU: Double = 0.5

    public fun score(
        truth: GroundTruth,
        result: RecognitionResult,
        order: OrderMode = OrderMode.XY_CUT,
    ): ImageScore {
        val native = result.blocks.flatMap { it.lines }
        val xyCut = ReadingOrder.sort(native) { it.corners ?: it.boundingBox.toQuad() }
        val predicted = native
        val strict = match(truth.lines.map { it.polygon }, predicted, LINE_IOU)
        val referenceText = truth.lines.joinToString(" ") { it.text }
        val nativePreserved = preserved(strict, native, native)
        val xyCutPreserved = preserved(strict, xyCut, native)
        val sequence = if (order == OrderMode.NATIVE) native else xyCut
        val orderedText = sequence.joinToString(" ") { it.text }
        val (wordHits, wordTextHits) = matchWords(truth, predicted)
        return ImageScore(
            id = truth.id,
            category = truth.category,
            tags = truth.tags,
            lighting = truth.lighting,
            truthLines = truth.lines.size,
            predictedLines = predicted.size,
            matchedLines = strict.size,
            truthWords = truth.lines.sumOf { it.words.size },
            wordBoxHits = wordHits,
            wordBoxAndTextHits = wordTextHits,
            ordered = TextErrors.of(referenceText, orderedText),
            bag = TextErrors.ofBag(referenceText, orderedText),
            predictedText = orderedText,
            orderPreserved = if (order == OrderMode.NATIVE) nativePreserved else xyCutPreserved,
            xyCutOrderPreserved = xyCutPreserved,
            nativeOrderPreserved = nativePreserved,
        )
    }

    private fun preserved(
        matches: Map<Int, Int>,
        sequence: List<TextLine>,
        native: List<TextLine>,
    ): Boolean {
        val positions = matches.toSortedMap().values.map { index -> sequence.indexOfFirst { it === native[index] } }
        return positions.zipWithNext().all { (a, b) -> a < b }
    }

    private fun match(
        truth: List<Quad>,
        predicted: List<TextLine>,
        threshold: Double,
    ): Map<Int, Int> {
        val pairs =
            buildList {
                for (t in truth.indices) {
                    for (p in predicted.indices) {
                        val iou = PolygonIou.of(truth[t], predicted[p].corners ?: predicted[p].boundingBox.toQuad())
                        if (iou >= threshold) add(Triple(iou, t, p))
                    }
                }
            }.sortedByDescending { it.first }
        val matched = linkedMapOf<Int, Int>()
        val used = mutableSetOf<Int>()
        for ((_, t, p) in pairs) {
            if (t in matched || p in used) continue
            matched[t] = p
            used.add(p)
        }
        return matched
    }

    private fun matchWords(
        truth: GroundTruth,
        predicted: List<TextLine>,
    ): Pair<Int, Int> {
        val predictedWords: List<TextWord> = predicted.flatMap { it.words }
        val candidates =
            buildList {
                for ((t, word) in truth.lines.flatMap { it.words }.withIndex()) {
                    for ((p, other) in predictedWords.withIndex()) {
                        val iou = PolygonIou.of(word.box, other.boundingBox)
                        if (iou >= WORD_IOU) add(Triple(iou, t, p))
                    }
                }
            }.sortedByDescending { it.first }
        val truthWords = truth.lines.flatMap { it.words }
        val usedTruth = mutableSetOf<Int>()
        val usedPredicted = mutableSetOf<Int>()
        var textHits = 0
        for ((_, t, p) in candidates) {
            if (t in usedTruth || p in usedPredicted) continue
            usedTruth.add(t)
            usedPredicted.add(p)
            if (TextMetrics.normalise(truthWords[t].text).lowercase() == TextMetrics.normalise(predictedWords[p].text).lowercase()) {
                textHits++
            }
        }
        return usedTruth.size to textHits
    }
}
