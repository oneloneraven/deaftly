package com.ravendarque.deaftly.core.ocr.bench

import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

public object ReadingOrder {
    private const val MIN_COLUMN_GAP_IN_LINE_HEIGHTS = 1.5f
    private const val MIN_COLUMN_SHARE = 0.25f
    private const val MIN_COLUMN_LINES = 2
    private const val SAME_ROW_IN_LINE_HEIGHTS = 0.5f

    public fun <T> sort(
        items: List<T>,
        quad: (T) -> Quad,
    ): List<T> {
        if (items.size <= 1) return items
        val skew = items.map { edgeAngle(quad(it)) }.sorted()[items.size / 2]
        val cos = cos(-skew)
        val sin = sin(-skew)
        val levelled =
            items.associateWith { item ->
                Quad(quad(item).points.map { Point(it.x * cos - it.y * sin, it.x * sin + it.y * cos) }).boundingBox()
            }
        val lineHeight =
            levelled.values
                .map { it.height }
                .sorted()[items.size / 2]
                .coerceAtLeast(1f)
        return cut(items, { levelled.getValue(it) }, lineHeight)
    }

    private fun edgeAngle(quad: Quad): Float {
        val (a, b) = quad.points
        return atan2(b.y - a.y, b.x - a.x)
    }

    private fun <T> cut(
        items: List<T>,
        box: (T) -> BoundingBox,
        lineHeight: Float,
    ): List<T> {
        if (items.size <= 1) return items
        val columns = bestSplit(items, { box(it).left }, { box(it).right }, MIN_COLUMN_GAP_IN_LINE_HEIGHTS * lineHeight, true)
        val parts = columns ?: bestSplit(items, { box(it).top }, { box(it).bottom }, 0f, false)
        return parts?.flatMap { cut(it, box, lineHeight) } ?: singleRow(items, box, lineHeight)
    }

    private fun <T> singleRow(
        items: List<T>,
        box: (T) -> BoundingBox,
        lineHeight: Float,
    ): List<T> {
        val byHeight = items.sortedBy { (box(it).top + box(it).bottom) / 2f }
        val rows = mutableListOf<MutableList<T>>()
        var rowCentre = Float.NEGATIVE_INFINITY
        for (item in byHeight) {
            val centre = (box(item).top + box(item).bottom) / 2f
            if (rows.isEmpty() || centre - rowCentre > SAME_ROW_IN_LINE_HEIGHTS * lineHeight) {
                rows.add(mutableListOf())
                rowCentre = centre
            }
            rows.last().add(item)
        }
        return rows.flatMap { row -> row.sortedBy { box(it).left } }
    }

    private fun <T> bestSplit(
        items: List<T>,
        start: (T) -> Float,
        end: (T) -> Float,
        minGap: Float,
        balanced: Boolean,
    ): List<List<T>>? {
        val sorted = items.sortedBy(start)
        val extentStart = start(sorted.first())
        val extent = sorted.maxOf(end) - extentStart
        val suffixEnd = FloatArray(sorted.size + 1) { Float.NEGATIVE_INFINITY }
        for (index in sorted.indices.reversed()) suffixEnd[index] = maxOf(suffixEnd[index + 1], end(sorted[index]))
        var reach = end(sorted.first())
        var best = -1
        var bestGap = minGap
        for (index in 1 until sorted.size) {
            val gap = start(sorted[index]) - reach
            if (gap > bestGap) {
                val left = reach - extentStart
                val right = suffixEnd[index] - start(sorted[index])
                val enoughLines = index >= MIN_COLUMN_LINES && sorted.size - index >= MIN_COLUMN_LINES
                val share = if (extent > 0f) minOf(left, right) / extent else 0f
                if (!balanced || (enoughLines && share >= MIN_COLUMN_SHARE)) {
                    best = index
                    bestGap = gap
                }
            }
            reach = maxOf(reach, end(sorted[index]))
        }
        return if (best < 0) null else listOf(sorted.subList(0, best), sorted.subList(best, sorted.size))
    }
}
