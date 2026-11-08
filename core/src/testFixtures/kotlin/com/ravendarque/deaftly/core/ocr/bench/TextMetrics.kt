package com.ravendarque.deaftly.core.ocr.bench

import java.text.Normalizer

public data class ErrorCount(
    val edits: Int,
    val reference: Int,
) {
    val rate: Double
        get() =
            when {
                reference > 0 -> edits.toDouble() / reference
                edits == 0 -> 0.0
                else -> 1.0
            }

    public operator fun plus(other: ErrorCount): ErrorCount = ErrorCount(edits + other.edits, reference + other.reference)

    public companion object {
        public val ZERO: ErrorCount = ErrorCount(0, 0)
    }
}

public object TextMetrics {
    public fun normalise(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC).trim().replace(WHITESPACE, " ")

    public fun characterErrors(
        reference: String,
        hypothesis: String,
        ignoreCase: Boolean,
    ): ErrorCount {
        val ref = prepare(reference, ignoreCase)
        val hyp = prepare(hypothesis, ignoreCase)
        return ErrorCount(editDistance(ref.toList(), hyp.toList()), ref.length)
    }

    public fun wordErrors(
        reference: String,
        hypothesis: String,
        ignoreCase: Boolean,
    ): ErrorCount {
        val ref = words(prepare(reference, ignoreCase))
        val hyp = words(prepare(hypothesis, ignoreCase))
        return ErrorCount(editDistance(ref, hyp), ref.size)
    }

    public fun bagErrors(
        reference: String,
        hypothesis: String,
        ignoreCase: Boolean,
    ): Pair<ErrorCount, ErrorCount> {
        val ref = words(prepare(reference, ignoreCase)).toMutableList()
        val hyp = words(prepare(hypothesis, ignoreCase)).toMutableList()
        val referenceChars = ref.sumOf { it.length }
        val referenceWords = ref.size
        for (word in ref.toList()) {
            if (hyp.remove(word)) ref.remove(word)
        }
        val pairs =
            buildList {
                for (r in ref.indices) {
                    for (h in hyp.indices) {
                        val distance = editDistance(ref[r].toList(), hyp[h].toList())
                        if (distance * 2 <= maxOf(ref[r].length, hyp[h].length)) add(Triple(distance, r, h))
                    }
                }
            }.sortedBy { it.first }
        val usedRef = mutableSetOf<Int>()
        val usedHyp = mutableSetOf<Int>()
        var charEdits = 0
        var wordEdits = 0
        for ((distance, r, h) in pairs) {
            if (r in usedRef || h in usedHyp) continue
            usedRef.add(r)
            usedHyp.add(h)
            charEdits += distance
            wordEdits++
        }
        charEdits += ref.indices.filter { it !in usedRef }.sumOf { ref[it].length }
        charEdits += hyp.indices.filter { it !in usedHyp }.sumOf { hyp[it].length }
        wordEdits += (ref.size - usedRef.size) + (hyp.size - usedHyp.size)
        return ErrorCount(charEdits, referenceChars) to ErrorCount(wordEdits, referenceWords)
    }

    public fun <T> editDistance(
        a: List<T>,
        b: List<T>,
    ): Int {
        if (a.isEmpty() || b.isEmpty()) return maxOf(a.size, b.size)
        var previous = IntArray(b.size + 1) { it }
        var current = IntArray(b.size + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val substitution = previous[j] + if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(substitution, previous[j + 1] + 1, current[j] + 1)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.size]
    }

    private fun prepare(
        text: String,
        ignoreCase: Boolean,
    ): String = normalise(text).let { if (ignoreCase) it.lowercase() else it }

    private fun words(text: String): List<String> = if (text.isEmpty()) emptyList() else text.split(' ')

    private val WHITESPACE = Regex("\\s+")
}
