package com.ravendarque.deaftly.core.ocr.bench

import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad

public object PolygonIou {
    public fun of(
        a: Quad,
        b: Quad,
    ): Double {
        val first = counterClockwise(a.points)
        val second = counterClockwise(b.points)
        val overlap = area(clip(first, second))
        val union = area(first) + area(second) - overlap
        return if (union <= 0.0) 0.0 else overlap / union
    }

    public fun of(
        a: BoundingBox,
        b: BoundingBox,
    ): Double {
        val width = minOf(a.right, b.right) - maxOf(a.left, b.left)
        val height = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
        if (width <= 0f || height <= 0f) return 0.0
        val overlap = width.toDouble() * height
        val union = a.width.toDouble() * a.height + b.width.toDouble() * b.height - overlap
        return if (union <= 0.0) 0.0 else overlap / union
    }

    private fun counterClockwise(points: List<Point>): List<Point> = if (signedArea(points) < 0.0) points.reversed() else points

    private fun signedArea(points: List<Point>): Double {
        var sum = 0.0
        for (i in points.indices) {
            val p = points[i]
            val q = points[(i + 1) % points.size]
            sum += p.x.toDouble() * q.y - q.x.toDouble() * p.y
        }
        return sum / 2.0
    }

    private fun area(points: List<Point>): Double = if (points.size < 3) 0.0 else kotlin.math.abs(signedArea(points))

    private fun clip(
        subject: List<Point>,
        clipper: List<Point>,
    ): List<Point> {
        var output = subject
        for (i in clipper.indices) {
            if (output.isEmpty()) break
            output = clipAgainstEdge(output, clipper[i], clipper[(i + 1) % clipper.size])
        }
        return output
    }

    private fun clipAgainstEdge(
        input: List<Point>,
        edgeStart: Point,
        edgeEnd: Point,
    ): List<Point> {
        val output = mutableListOf<Point>()
        for (j in input.indices) {
            val current = input[j]
            val previous = input[(j + input.size - 1) % input.size]
            val currentInside = inside(current, edgeStart, edgeEnd)
            val previousInside = inside(previous, edgeStart, edgeEnd)
            if (currentInside != previousInside) output.add(intersection(previous, current, edgeStart, edgeEnd))
            if (currentInside) output.add(current)
        }
        return output
    }

    private fun inside(
        point: Point,
        start: Point,
        end: Point,
    ): Boolean = (end.x - start.x) * (point.y - start.y) - (end.y - start.y) * (point.x - start.x) >= 0f

    private fun intersection(
        a: Point,
        b: Point,
        start: Point,
        end: Point,
    ): Point {
        val dx = (b.x - a.x).toDouble()
        val dy = (b.y - a.y).toDouble()
        val ex = (end.x - start.x).toDouble()
        val ey = (end.y - start.y).toDouble()
        val denominator = dx * ey - dy * ex
        if (denominator == 0.0) return a
        val t = ((start.x - a.x) * ey - (start.y - a.y) * ex) / denominator
        return Point((a.x + t * dx).toFloat(), (a.y + t * dy).toFloat())
    }
}
