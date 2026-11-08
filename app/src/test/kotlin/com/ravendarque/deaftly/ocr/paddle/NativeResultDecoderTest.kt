package com.ravendarque.deaftly.ocr.paddle

import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NativeResultDecoderTest {
    @Test
    fun decodesLineAndWordQuadsInTheNativeLayout() {
        val geometry =
            floatArrayOf(
                0.9f,
                10f,
                10f,
                110f,
                20f,
                109f,
                40f,
                9f,
                30f,
                2f,
                0.8f,
                10f,
                10f,
                50f,
                14f,
                49f,
                34f,
                9f,
                30f,
                0.7f,
                60f,
                16f,
                110f,
                20f,
                109f,
                40f,
                59f,
                36f,
            )
        val texts = arrayOf("two words", "two", "words")

        val result = NativeResultDecoder.decode(geometry, texts, 1)

        val line =
            result.blocks
                .single()
                .lines
                .single()
        assertEquals("two words", line.text)
        assertEquals(0.9f, line.confidence)
        assertEquals(BoundingBox(9f, 10f, 110f, 40f), line.boundingBox)
        assertEquals(Math.toDegrees(kotlin.math.atan2(10.0, 100.0)).toFloat(), line.angleDegrees)
        assertEquals(listOf("two", "words"), line.words.map { it.text })
        val second = line.words[1]
        assertEquals(
            listOf(Point(60f, 16f), Point(110f, 20f), Point(109f, 40f), Point(59f, 36f)),
            assertNotNull(second.corners).points,
        )
        assertEquals(BoundingBox(59f, 16f, 110f, 40f), second.boundingBox)
        assertEquals(0.7f, second.confidence)
    }

    @Test
    fun decodesNothingToAnEmptyResult() {
        assertEquals(0, NativeResultDecoder.decode(floatArrayOf(), emptyArray(), 0).blocks.size)
    }
}
