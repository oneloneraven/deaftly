package com.ravendarque.deaftly.ocr

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.LiveRecognition
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.bench.OcrBenchmark
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrConfig
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import kotlin.test.assertTrue

/**
 * Measures the live screen's per-frame analysis cost: a camera-shaped frame (960 by 720, rotated a quarter turn, as CameraX
 * delivers it for a portrait phone) goes through recognition, viewport mapping and tracking. The figures are for this device
 * only; requirement N2 (under 500 ms on the reference phone) needs a real mid-range phone (#54).
 */
@RunWith(AndroidJUnit4::class)
class LiveRecognitionLatencyTest {
    private fun cameraFrame(source: Bitmap): RecognitionFrame {
        val landscape =
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(-QUARTER_TURN) }, true)
        val scaled = Bitmap.createScaledBitmap(landscape, FRAME_WIDTH, FRAME_HEIGHT, true)
        val copy = scaled.copy(Bitmap.Config.ARGB_8888, false)
        val buffer = ByteBuffer.allocateDirect(copy.byteCount)
        copy.copyPixelsToBuffer(buffer)
        buffer.rewind()
        return RecognitionFrame(copy.width, copy.height, QUARTER_TURN.toInt(), PixelFormat.RGBA_8888, buffer, copy.rowBytes)
    }

    @Test
    fun reportsPerFrameLatencyAtDetection960() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val frames = FixtureSet.load().map { cameraFrame(it.bitmap) }
        val crop = BoundingBox(0f, 0f, FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat())
        val latencies = mutableListOf<Double>()
        var phraseCount = 0
        PaddleOcrNcnnRecogniser(context, PaddleOcrConfig(detectionSide = DETECTION_SIDE)).use { recogniser ->
            val live = LiveRecognition(recogniser)
            frames.forEach { frame ->
                repeat(WARM_UPS) { runBlocking { live.process(frame, crop) } }
                repeat(TIMED_RUNS) {
                    val start = System.nanoTime()
                    val phrases = runBlocking { live.process(frame, crop) }
                    latencies += (System.nanoTime() - start) / NANOS_PER_MILLI
                    phraseCount = phrases.size
                }
            }
        }
        val sorted = latencies.sorted()
        val median = sorted[sorted.size / 2]
        val p95 = sorted[(sorted.size * P95).toInt().coerceAtMost(sorted.lastIndex)]
        val summary =
            "Live analysis latency, ${frames.size} fixture frames x $TIMED_RUNS runs, detection ${DETECTION_SIDE}px, " +
                "${Build.MODEL} (${Build.SUPPORTED_ABIS.first()}): median %.0f ms, p95 %.0f ms, max %.0f ms".format(
                    median,
                    p95,
                    sorted.last(),
                )
        Log.i(TAG, summary)
        File(OcrBenchmark.outputDirectory().also { it.mkdirs() }, "live-latency.txt").writeText(summary + "\n")
        assertTrue(phraseCount > 0, "The last frame produced no phrases")
        assertTrue(p95 < MAX_P95_MILLIS, "p95 $p95 ms is far beyond what the live screen can use")
    }

    private companion object {
        const val TAG = "LiveLatency"
        const val FRAME_WIDTH = 960
        const val FRAME_HEIGHT = 720
        const val QUARTER_TURN = 90f
        const val DETECTION_SIDE = 960
        const val WARM_UPS = 1
        const val TIMED_RUNS = 3
        const val NANOS_PER_MILLI = 1_000_000.0
        const val P95 = 0.95
        const val MAX_P95_MILLIS = 5_000.0
    }
}
