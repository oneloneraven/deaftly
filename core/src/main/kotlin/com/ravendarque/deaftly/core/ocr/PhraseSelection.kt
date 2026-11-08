package com.ravendarque.deaftly.core.ocr

import kotlin.math.hypot

public data class WordRef(
    val phraseId: Long,
    val word: Int,
)

/** A run of words the user has picked, from [anchor] to [focus] in either direction. */
public data class TextSelection(
    val anchor: WordRef,
    val focus: WordRef,
)

public class SelectedRun(
    public val phrase: Phrase,
    public val firstWord: Int,
    public val lastWord: Int,
) {
    public val coversPhrase: Boolean get() = firstWord == 0 && lastWord == phrase.words.lastIndex
}

/** Reading order is the order of the phrases list, which is the recogniser's line order. */
public object PhraseSelection {
    public fun wholePhrase(phrase: Phrase): TextSelection = TextSelection(WordRef(phrase.id, 0), WordRef(phrase.id, phrase.words.lastIndex))

    public fun runs(
        phrases: List<Phrase>,
        selection: TextSelection,
    ): List<SelectedRun> {
        val a = locate(phrases, selection.anchor)
        val b = locate(phrases, selection.focus)
        if (a == null || b == null) return emptyList()
        val (start, end) = if (a.first < b.first || (a.first == b.first && a.second <= b.second)) a to b else b to a
        return (start.first..end.first).map { index ->
            val phrase = phrases[index]
            val first = if (index == start.first) start.second else 0
            val last = if (index == end.first) end.second else phrase.words.lastIndex
            SelectedRun(phrase, first, last)
        }
    }

    public fun text(
        phrases: List<Phrase>,
        selection: TextSelection,
    ): String? {
        val runs = runs(phrases, selection)
        if (runs.isEmpty()) return null
        return runs.joinToString(" ") { run ->
            if (run.coversPhrase) {
                run.phrase.text
            } else {
                run.phrase.words
                    .subList(
                        run.firstWord,
                        run.lastWord + 1,
                    ).joinToString(" ") { it.text }
            }
        }
    }

    private fun locate(
        phrases: List<Phrase>,
        ref: WordRef,
    ): Pair<Int, Int>? {
        val index = phrases.indexOfFirst { it.id == ref.phraseId }
        return if (index >= 0 && ref.word in phrases[index].words.indices) index to ref.word else null
    }
}

public object PhraseHitTest {
    /**
     * The phrase nearest to the [touch] in a view of [width] by [height] pixels, if
     * the touch is inside it or within [slop] pixels of it. Inside beats near; of two near, the closer wins.
     */
    public fun phraseAt(
        phrases: List<Phrase>,
        touch: Point,
        width: Float,
        height: Float,
        slop: Float,
    ): Phrase? =
        phrases
            .map { it to distance(it.quad, touch.x, touch.y, width, height) }
            .filter { it.second <= slop }
            .minByOrNull { it.second }
            ?.first

    internal fun distance(
        quad: Quad,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): Float {
        val corners = quad.points.map { Point(it.x * width, it.y * height) }
        if (inside(corners, x, y)) return 0f
        return corners.indices.minOf { i -> segmentDistance(corners[i], corners[(i + 1) % corners.size], x, y) }
    }

    private fun inside(
        corners: List<Point>,
        x: Float,
        y: Float,
    ): Boolean {
        var inside = false
        var j = corners.lastIndex
        for (i in corners.indices) {
            val a = corners[i]
            val b = corners[j]
            if ((a.y > y) != (b.y > y) && x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x) inside = !inside
            j = i
        }
        return inside
    }

    private fun segmentDistance(
        a: Point,
        b: Point,
        x: Float,
        y: Float,
    ): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0f) 0f else (((x - a.x) * dx + (y - a.y) * dy) / lengthSquared).coerceIn(0f, 1f)
        return hypot(x - (a.x + t * dx), y - (a.y + t * dy))
    }
}
