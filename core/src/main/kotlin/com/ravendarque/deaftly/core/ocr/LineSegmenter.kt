package com.ravendarque.deaftly.core.ocr

import kotlin.math.hypot

/**
 * Splits a recognised line into the parts a reader takes as separate, such as a menu item and its
 * price. A run of leader words is dropped and splits the line when it holds at least
 * [MIN_LEADER_DOTS] dots, or when it is made of dots and the gap across it is wider than
 * [LEADER_GAP_RATIO] times the line's height; a gap with no leaders splits when wider than
 * [MAX_GAP_RATIO]. The recogniser often reads the space before a price as a few dots, which is why
 * short runs count. Works in frame pixels, before viewport mapping, so gaps and height share units.
 */
internal object LineSegmenter {
    const val MAX_GAP_RATIO: Float = 1.0f
    const val LEADER_GAP_RATIO: Float = 0.75f
    const val MIN_LEADER_DOTS: Int = 4
    private const val ELLIPSIS_DOTS = 3
    private const val LEADER_CHARACTERS = ".…·_"
    private const val DOT_CHARACTERS = ".…_"
    private const val TOP_LEFT = 0
    private const val TOP_RIGHT = 1
    private const val BOTTOM_RIGHT = 2
    private const val BOTTOM_LEFT = 3

    fun split(line: TextLine): List<TextLine> {
        val words = line.words
        val height = height(line)
        val segments = mutableListOf(mutableListOf<TextWord>())
        var i = 0
        while (i < words.size) {
            var end = i
            while (end < words.size && isLeaderCharacters(words[end].text)) end++
            if (end == i) {
                val previous = segments.last().lastOrNull()
                if (previous != null && gap(previous, words[i]) > height * MAX_GAP_RATIO) segments += mutableListOf<TextWord>()
                segments.last() += words[i]
                i++
                continue
            }
            val run = words.subList(i, end)
            val leader = isLeader(run, segments.last().lastOrNull(), words.getOrNull(end), height)
            if (leader) segments += mutableListOf<TextWord>() else segments.last() += run
            i = end
        }
        val parts = segments.filter { it.isNotEmpty() }
        val unchanged = parts.size == 1 && parts.single().size == words.size
        return if (parts.isEmpty() || unchanged) listOf(line) else parts.map { segment(line, it) }
    }

    private fun isLeader(
        run: List<TextWord>,
        previous: TextWord?,
        next: TextWord?,
        height: Float,
    ): Boolean =
        run.sumOf { dots(it.text) } >= MIN_LEADER_DOTS ||
            (
                previous != null &&
                    next != null &&
                    run.all { word -> word.text.all { it in DOT_CHARACTERS } } &&
                    gap(previous, next) > height * LEADER_GAP_RATIO
            )

    private fun isLeaderCharacters(text: String): Boolean = text.isNotEmpty() && text.all { it in LEADER_CHARACTERS }

    private fun dots(text: String): Int = text.sumOf { if (it == '…') ELLIPSIS_DOTS else 1 }

    private fun segment(
        line: TextLine,
        words: List<TextWord>,
    ): TextLine {
        val first = quad(words.first()).points
        val last = quad(words.last()).points
        val corners = Quad(listOf(first[TOP_LEFT], last[TOP_RIGHT], last[BOTTOM_RIGHT], first[BOTTOM_LEFT]))
        return line.copy(
            text = words.joinToString(" ") { it.text },
            boundingBox = corners.boundingBox(),
            words = words,
            corners = line.corners?.let { corners },
        )
    }

    private fun height(line: TextLine): Float {
        val corners = line.corners?.points ?: return line.boundingBox.height
        return (distance(corners[TOP_LEFT], corners[BOTTOM_LEFT]) + distance(corners[TOP_RIGHT], corners[BOTTOM_RIGHT])) / 2
    }

    private fun gap(
        before: TextWord,
        after: TextWord,
    ): Float {
        val a = quad(before).points
        val b = quad(after).points
        val endOfBefore = midpoint(a[TOP_RIGHT], a[BOTTOM_RIGHT])
        val startOfAfter = midpoint(b[TOP_LEFT], b[BOTTOM_LEFT])
        val baseline = Point(a[TOP_RIGHT].x - a[TOP_LEFT].x, a[TOP_RIGHT].y - a[TOP_LEFT].y)
        val length = hypot(baseline.x, baseline.y)
        if (length == 0f) return distance(endOfBefore, startOfAfter)
        return ((startOfAfter.x - endOfBefore.x) * baseline.x + (startOfAfter.y - endOfBefore.y) * baseline.y) / length
    }

    private fun quad(word: TextWord): Quad = word.corners ?: word.boundingBox.toQuad()

    private fun midpoint(
        a: Point,
        b: Point,
    ) = Point((a.x + b.x) / 2, (a.y + b.y) / 2)

    private fun distance(
        a: Point,
        b: Point,
    ) = hypot(b.x - a.x, b.y - a.y)
}
