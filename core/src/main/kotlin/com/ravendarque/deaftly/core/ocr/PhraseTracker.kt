package com.ravendarque.deaftly.core.ocr

import kotlin.math.max
import kotlin.math.min

/**
 * Keeps phrase identities and positions steady from one frame to the next: a detection that overlaps
 * a known phrase by at least [minOverlap] (intersection over union of the bounding boxes) is the same
 * phrase, its corners move [smoothing] of the way to the new position, and a phrase missed for more
 * than [maxMisses] consecutive frames is dropped.
 */
public class PhraseTracker(
    private val minOverlap: Float = DEFAULT_MIN_OVERLAP,
    private val smoothing: Float = DEFAULT_SMOOTHING,
    private val maxMisses: Int = DEFAULT_MAX_MISSES,
) {
    private class Known(
        val phrase: Phrase,
        val misses: Int,
    )

    private var known: List<Known> = emptyList()
    private var nextId = 1L

    init {
        require(minOverlap in 0f..1f && smoothing in 0f..1f && maxMisses >= 0) { "Tracker settings out of range" }
    }

    public fun update(detected: List<DetectedPhrase>): List<Phrase> {
        val matches = match(detected)
        val matchedKnown = matches.values.toSet()
        val current =
            detected.mapIndexed { index, candidate ->
                val previous = matches[index]?.let { known[it].phrase }
                if (previous == null) {
                    Phrase(nextId++, candidate.text, candidate.quad, candidate.words)
                } else {
                    blend(previous, candidate)
                }
            }
        val kept = known.filterIndexed { index, _ -> index !in matchedKnown }.filter { it.misses < maxMisses }
        known = current.map { Known(it, 0) } + kept.map { Known(it.phrase, it.misses + 1) }
        return known.map { it.phrase }
    }

    public fun reset() {
        known = emptyList()
    }

    private fun match(detected: List<DetectedPhrase>): Map<Int, Int> {
        val pairs =
            buildList {
                detected.forEachIndexed { d, candidate ->
                    known.forEachIndexed { k, existing ->
                        val overlap = overlap(candidate.quad.boundingBox(), existing.phrase.quad.boundingBox())
                        if (overlap >= minOverlap) add(Triple(overlap, d, k))
                    }
                }
            }.sortedByDescending { it.first }
        val result = mutableMapOf<Int, Int>()
        val taken = mutableSetOf<Int>()
        for ((_, d, k) in pairs) {
            if (d !in result && k !in taken) {
                result[d] = k
                taken += k
            }
        }
        return result
    }

    private fun blend(
        previous: Phrase,
        candidate: DetectedPhrase,
    ): Phrase {
        val words =
            if (previous.words.size == candidate.words.size) {
                candidate.words.mapIndexed { i, word -> PhraseWord(word.text, ease(previous.words[i].quad, word.quad)) }
            } else {
                candidate.words
            }
        return Phrase(previous.id, candidate.text, ease(previous.quad, candidate.quad), words)
    }

    private fun ease(
        from: Quad,
        to: Quad,
    ): Quad =
        Quad(
            from.points.zip(to.points) { a, b ->
                Point(a.x + (b.x - a.x) * smoothing, a.y + (b.y - a.y) * smoothing)
            },
        )

    private fun overlap(
        a: BoundingBox,
        b: BoundingBox,
    ): Float {
        val width = min(a.right, b.right) - max(a.left, b.left)
        val height = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (width <= 0f || height <= 0f) return 0f
        val intersection = width * height
        val union = a.width * a.height + b.width * b.height - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    public companion object {
        public const val DEFAULT_MIN_OVERLAP: Float = 0.4f
        public const val DEFAULT_SMOOTHING: Float = 0.6f
        public const val DEFAULT_MAX_MISSES: Int = 2
    }
}
