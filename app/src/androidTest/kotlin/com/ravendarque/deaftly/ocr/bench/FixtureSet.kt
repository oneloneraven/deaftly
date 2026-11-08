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
