package com.ravendarque.deaftly.core.ocr

import kotlin.math.hypot

/**
 * Splits a recognised line into the parts a reader takes as separate, such as a menu item and its
 * price: at a word made only of leader characters, which is dropped, and at a gap between words
 * wider than [MAX_GAP_RATIO] times the line's height. Works in frame pixels, before viewport
 * mapping, so the gap and the height are measured in the same units.
 */
public object LineSegmenter {
    public const val MAX_GAP_RATIO: Float = 1.5f
    private const val LEADER_CHARACTERS = ".…·_"
    private const val TOP_LEFT = 0
    private const val TOP_RIGHT = 1
    private const val BOTTOM_RIGHT = 2
    private const val BOTTOM_LEFT = 3

    public fun split(line: TextLine): List<TextLine> {
        if (line.words.isEmpty()) return listOf(line)
        val height = height(line)
        val segments = mutableListOf<MutableList<TextWord>>()
        var broken = true
        var changed = false
        for (word in line.words) {
            if (isLeader(word.text)) {
                broken = true
                changed = true
                continue
            }
            val previous = segments.lastOrNull()?.last()
            if (!broken && previous != null && gap(previous, word) > height * MAX_GAP_RATIO) broken = true
            if (broken) {
                if (segments.isNotEmpty()) changed = true
                segments += mutableListOf(word)
            } else {
                segments.last() += word
            }
            broken = false
        }
        return if (segments.isEmpty() || !changed) listOf(line) else segments.map { segment(line, it) }
    }

    internal fun isLeader(text: String): Boolean = text.length >= 2 && text.all { it in LEADER_CHARACTERS }

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
