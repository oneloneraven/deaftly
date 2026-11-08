package com.ravendarque.deaftly.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometryTest {
    @Test
    fun quadBoundingBoxCoversRotatedCorners() {
        val quad = Quad(listOf(Point(2f, 0f), Point(6f, 1f), Point(5f, 4f), Point(1f, 3f)))
        assertEquals(BoundingBox(1f, 0f, 6f, 4f), quad.boundingBox())
    }

    @Test
    fun boxToQuadGoesClockwiseFromTopLeft() {
        assertEquals(
            listOf(Point(1f, 2f), Point(3f, 2f), Point(3f, 5f), Point(1f, 5f)),
            BoundingBox(1f, 2f, 3f, 5f).toQuad().points,
        )
    }

    @Test
    fun boxReportsSizeAndContainment() {
        val box = BoundingBox(1f, 2f, 3f, 5f)
        assertEquals(2f, box.width)
        assertEquals(3f, box.height)
        assertTrue(box.contains(Point(2f, 3f)))
        assertFalse(box.contains(Point(0f, 3f)))
    }

    @Test
    fun rejectsInvertedBoxesAndShortQuads() {
        assertFailsWith<IllegalArgumentException> { BoundingBox(3f, 0f, 1f, 1f) }
        assertFailsWith<IllegalArgumentException> { Quad(listOf(Point(0f, 0f))) }
    }
}
