package com.ravendarque.deaftly.ui.ocr

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import java.nio.ByteBuffer

private const val BYTES_PER_PIXEL = 4

object FrameImage {
    /** The part of [frame] the user sees, turned upright, as a bitmap. Reads the frame's pixels; copies them. */
    fun toBitmap(
        frame: RecognitionFrame,
        crop: BoundingBox?,
    ): Bitmap {
        val source = createBitmap(frame.width, frame.height)
        source.copyPixelsFromBuffer(packed(frame))
        val left = (crop?.left ?: 0f).toInt().coerceIn(0, frame.width - 1)
        val top = (crop?.top ?: 0f).toInt().coerceIn(0, frame.height - 1)
        val right = (crop?.right ?: frame.width.toFloat()).toInt().coerceIn(left + 1, frame.width)
        val bottom = (crop?.bottom ?: frame.height.toFloat()).toInt().coerceIn(top + 1, frame.height)
        val matrix = Matrix().apply { postRotate(frame.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top, matrix, true)
    }

    private fun packed(frame: RecognitionFrame): ByteBuffer {
        val rowBytes = frame.width * BYTES_PER_PIXEL
        val source = frame.pixels.duplicate()
        val out = ByteBuffer.allocate(rowBytes * frame.height)
        val row = ByteArray(rowBytes)
        for (y in 0 until frame.height) {
            source.position(y * frame.rowStrideBytes)
            source.get(row)
            out.put(row)
        }
        out.rewind()
        return out
    }
}
