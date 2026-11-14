package com.ravendarque.deaftly.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.LiveRecognition
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.ViewportMapping
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrConfig
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks the line-splitting thresholds against what the real recogniser reads, not hand-made geometry. */
@RunWith(AndroidJUnit4::class)
class MenuPriceSplitTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun phrases(frame: RecognitionFrame): List<String> =
        PaddleOcrNcnnRecogniser(context, PaddleOcrConfig(detectionSide = DETECTION_SIDE)).use { recogniser ->
            val result = runBlocking { recogniser.recognise(frame) }
            val visible = BoundingBox(0f, 0f, frame.width.toFloat(), frame.height.toFloat())
            LiveRecognition(recogniser).detect(result, ViewportMapping.of(frame, visible)).map { it.text }
        }

    @Test
    fun menuItemsAreNeverBoxedWithTheirPrices() {
        val menus = FixtureSet.load().filter { it.truth.id in MENUS }
        assertEquals(MENUS.size, menus.size)
        val merged = menus.flatMap { phrases(it.frame) }.filter { ITEM_THEN_PRICE.containsMatchIn(it) }

        assertTrue(merged.isEmpty(), "Item and price share a box: $merged")
    }

    @Test
    fun everyLeaderStyleSplitsAndAnEllipsisDoesNot() {
        val samples =
            listOf(
                "Penne arrabbiata..........11.00",
                "Penne arrabbiata . . . . . . . 11.00",
                "Penne arrabbiata …… 11.00",
                "Back soon... sorry",
            )
        val bitmap = Bitmap.createBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT, Bitmap.Config.ARGB_8888)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = SAMPLE_TEXT_SIZE
                typeface = Typeface.SERIF
            }
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            samples.forEachIndexed { i, sample -> drawText(sample, SAMPLE_LEFT, SAMPLE_TOP + i * SAMPLE_PITCH, paint) }
        }
        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount).also { bitmap.copyPixelsToBuffer(it) }.rewind() as ByteBuffer

        val texts = phrases(RecognitionFrame(bitmap.width, bitmap.height, 0, PixelFormat.RGBA_8888, buffer, bitmap.rowBytes))

        assertEquals(3, texts.count { it == "11.00" }, "Phrases: $texts")
        assertTrue(texts.none { ITEM_THEN_PRICE.containsMatchIn(it) }, "Phrases: $texts")
        assertTrue("Back soon... sorry" in texts, "Phrases: $texts")
    }

    private companion object {
        const val DETECTION_SIDE = 960
        const val SAMPLE_WIDTH = 960
        const val SAMPLE_HEIGHT = 600
        const val SAMPLE_TEXT_SIZE = 40f
        const val SAMPLE_LEFT = 60f
        const val SAMPLE_TOP = 120f
        const val SAMPLE_PITCH = 120f
        val MENUS = setOf("menu-trattoria", "menu-tapas")
        val ITEM_THEN_PRICE = Regex("""\p{L}{3,}.*\s£?\d+[.,]\d{2}$""")
    }
}
