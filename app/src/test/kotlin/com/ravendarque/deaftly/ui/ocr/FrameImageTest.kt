package com.ravendarque.deaftly.ui.ocr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.nio.ByteBuffer
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FrameImageTest {
    private fun frame(
        rotation: Int,
        stride: Int = 16,
    ): RecognitionFrame {
        val buffer = ByteBuffer.allocate(stride * 2)
        repeat(2) { y ->
            repeat(4) { x ->
                val red = x == 0 && y == 0
                buffer.position(y * stride + x * 4)
                buffer.put(byteArrayOf(if (red) -1 else 0, 0, if (red) 0 else -1, -1))
            }
        }
        return RecognitionFrame(4, 2, rotation, PixelFormat.RGBA_8888, buffer, stride)
    }

    private fun marked(bitmap: Bitmap): List<Pair<Int, Int>> {
        val background = bitmap.getPixel(bitmap.width - 1, bitmap.height - 1)
        return (0 until bitmap.height)
            .flatMap { y -> (0 until bitmap.width).map { x -> x to y } }
            .filter { (x, y) -> bitmap.getPixel(x, y) != background }
    }

    @Test
    fun anUprightFrameKeepsItsSizeAndPixels() {
        val bitmap = FrameImage.toBitmap(frame(0), null)

        assertEquals(4, bitmap.width)
        assertEquals(2, bitmap.height)
        assertEquals(listOf(0 to 0), marked(bitmap))
    }

    @Test
    fun aQuarterTurnFrameIsTurnedUpright() {
        val bitmap = FrameImage.toBitmap(frame(90), null)

        assertEquals(2, bitmap.width)
        assertEquals(4, bitmap.height)
        assertEquals(listOf(1 to 0), marked(bitmap))
    }

    @Test
    fun onlyTheCropIsKept() {
        val bitmap = FrameImage.toBitmap(frame(0), BoundingBox(2f, 0f, 4f, 2f))

        assertEquals(2, bitmap.width)
        assertEquals(2, bitmap.height)
        assertEquals(emptyList(), marked(bitmap))
    }

    @Test
    fun rowPaddingIsSkipped() {
        val bitmap = FrameImage.toBitmap(frame(0, stride = 24), null)

        assertEquals(listOf(0 to 0), marked(bitmap))
    }
}
