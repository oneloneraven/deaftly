package com.ravendarque.deaftly.core.ocr

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecognitionFrameTest {
    private fun frame(
        rotation: Int,
        width: Int = 8,
        height: Int = 4,
    ) = RecognitionFrame(
        width = width,
        height = height,
        rotationDegrees = rotation,
        format = PixelFormat.RGBA_8888,
        pixels = ByteBuffer.allocate(width * height * 4),
    )

    @Test
    fun uprightSizeSwapsOnQuarterTurns() {
        assertEquals(8 to 4, frame(0).let { it.uprightWidth to it.uprightHeight })
        assertEquals(4 to 8, frame(90).let { it.uprightWidth to it.uprightHeight })
        assertEquals(8 to 4, frame(180).let { it.uprightWidth to it.uprightHeight })
        assertEquals(4 to 8, frame(270).let { it.uprightWidth to it.uprightHeight })
    }

    @Test
    fun pointsMapThroughEachRotation() {
        val point = Point(1f, 0f)
        assertEquals(Point(1f, 0f), frame(0).toUpright(point))
        assertEquals(Point(4f, 1f), frame(90).toUpright(point))
        assertEquals(Point(7f, 4f), frame(180).toUpright(point))
        assertEquals(Point(0f, 7f), frame(270).toUpright(point))
    }

    @Test
    fun originOfRawFrameLandsOnExpectedUprightCorner() {
        val origin = Point(0f, 0f)
        assertEquals(Point(4f, 0f), frame(90).toUpright(origin))
        assertEquals(Point(8f, 4f), frame(180).toUpright(origin))
        assertEquals(Point(0f, 8f), frame(270).toUpright(origin))
    }

    @Test
    fun ninetyThenTwoSeventyIsIdentityAcrossTheSwappedFrame() {
        val point = Point(3f, 1f)
        val turned = frame(90).toUpright(point)
        val back = frame(270, width = 4, height = 8).toUpright(turned)
        assertEquals(point, back)
    }

    @Test
    fun boxesStayAxisAligned() {
        val box = BoundingBox(1f, 0f, 3f, 1f)
        assertEquals(BoundingBox(3f, 1f, 4f, 3f), frame(90).toUpright(box))
        assertEquals(BoundingBox(5f, 3f, 7f, 4f), frame(180).toUpright(box))
        assertEquals(BoundingBox(0f, 5f, 1f, 7f), frame(270).toUpright(box))
    }

    @Test
    fun quadsKeepTheirCornerOrder() {
        val quad = BoundingBox(1f, 0f, 3f, 1f).toQuad()
        assertEquals(
            listOf(Point(4f, 1f), Point(4f, 3f), Point(3f, 3f), Point(3f, 1f)),
            frame(90).toUpright(quad).points,
        )
    }

    @Test
    fun rejectsInvalidFrames() {
        assertFailsWith<IllegalArgumentException> { frame(45) }
        assertFailsWith<IllegalArgumentException> { frame(0, width = 0) }
        assertFailsWith<IllegalArgumentException> {
            RecognitionFrame(8, 4, 0, PixelFormat.RGBA_8888, ByteBuffer.allocate(8))
        }
        assertFailsWith<IllegalArgumentException> {
            RecognitionFrame(8, 4, 0, PixelFormat.NV21, ByteBuffer.allocate(8 * 4 * 3 / 2 - 1))
        }
        assertFailsWith<IllegalArgumentException> {
            RecognitionFrame(7, 4, 0, PixelFormat.NV21, ByteBuffer.allocate(100))
        }
    }

    @Test
    fun acceptsPaddedRowsWithoutTrailingPadding() {
        RecognitionFrame(8, 4, 0, PixelFormat.RGBA_8888, ByteBuffer.allocate(40 * 3 + 32), rowStrideBytes = 40)
    }
}
