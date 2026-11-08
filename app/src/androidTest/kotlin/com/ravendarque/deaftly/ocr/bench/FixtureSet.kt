package com.ravendarque.deaftly.ocr.bench

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.bench.GroundTruth
import java.nio.ByteBuffer

class Fixture(
    val truth: GroundTruth,
    val bitmap: Bitmap,
) {
    val frame: RecognitionFrame by lazy {
        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()
        RecognitionFrame(bitmap.width, bitmap.height, 0, PixelFormat.RGBA_8888, buffer, bitmap.rowBytes)
    }

    val nv21Frame: RecognitionFrame by lazy {
        val width = bitmap.width
        val height = bitmap.height
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)
        val nv21 = ByteArray(width * height * NV21_BYTES_PER_PIXEL_NUMERATOR / NV21_BYTES_PER_PIXEL_DENOMINATOR)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val (r, g, b) = rgb(argb[y * width + x])
                nv21[y * width + x] = clamp(((Y_R * r + Y_G * g + Y_B * b) shr FIXED_POINT_SHIFT) + Y_OFFSET).toByte()
                if (y % 2 == 0 && x % 2 == 0) {
                    val chroma = width * height + (y / 2) * width + x
                    nv21[chroma] = clamp(((V_R * r + V_G * g + V_B * b) shr FIXED_POINT_SHIFT) + CHROMA_OFFSET).toByte()
                    nv21[chroma + 1] = clamp(((U_R * r + U_G * g + U_B * b) shr FIXED_POINT_SHIFT) + CHROMA_OFFSET).toByte()
                }
            }
        }
        val buffer = ByteBuffer.allocateDirect(nv21.size)
        buffer.put(nv21)
        buffer.rewind()
        RecognitionFrame(width, height, 0, PixelFormat.NV21, buffer)
    }

    private fun rgb(argb: Int): Triple<Int, Int, Int> =
        Triple((argb shr RED_SHIFT) and BYTE, (argb shr GREEN_SHIFT) and BYTE, argb and BYTE)

    private fun clamp(value: Int): Int = value.coerceIn(0, BYTE)

    private companion object {
        const val BYTE = 0xFF
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val FIXED_POINT_SHIFT = 8
        const val NV21_BYTES_PER_PIXEL_NUMERATOR = 3
        const val NV21_BYTES_PER_PIXEL_DENOMINATOR = 2
        const val Y_OFFSET = 16
        const val CHROMA_OFFSET = 128
        const val Y_R = 66
        const val Y_G = 129
        const val Y_B = 25
        const val U_R = -38
        const val U_G = -74
        const val U_B = 112
        const val V_R = 112
        const val V_G = -94
        const val V_B = -18
    }
}

object FixtureSet {
    private const val DIRECTORY = "ocr-fixtures"

    fun load(): List<Fixture> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return assets
            .list(DIRECTORY)
            .orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { name ->
                val id = name.removeSuffix(".json")
                val json = assets.open("$DIRECTORY/$name").use { it.readBytes().toString(Charsets.UTF_8) }
                val bitmap = assets.open("$DIRECTORY/$id.jpg").use { BitmapFactory.decodeStream(it, null, options) }
                Fixture(GroundTruth.parse(id, json), checkNotNull(bitmap) { "Cannot decode $id" })
            }
    }
}
