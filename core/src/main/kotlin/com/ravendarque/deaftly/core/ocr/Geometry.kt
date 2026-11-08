package com.ravendarque.deaftly.core.ocr

public data class Point(
    val x: Float,
    val y: Float,
)

public data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(right >= left && bottom >= top) { "Box must not be inverted" }
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top

    public fun contains(point: Point): Boolean = point.x in left..right && point.y in top..bottom

    public fun toQuad(): Quad =
        Quad(
            listOf(Point(left, top), Point(right, top), Point(right, bottom), Point(left, bottom)),
        )
}

public data class Quad(
    val points: List<Point>,
) {
    init {
        require(points.size == CORNERS) { "A quad has exactly $CORNERS corners" }
    }

    public fun boundingBox(): BoundingBox =
        BoundingBox(
            left = points.minOf { it.x },
            top = points.minOf { it.y },
            right = points.maxOf { it.x },
            bottom = points.maxOf { it.y },
        )

    private companion object {
        const val CORNERS = 4
    }
}
