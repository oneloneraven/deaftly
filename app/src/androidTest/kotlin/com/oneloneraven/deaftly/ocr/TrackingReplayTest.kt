package com.oneloneraven.deaftly.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oneloneraven.deaftly.core.ocr.BoundingBox
import com.oneloneraven.deaftly.core.ocr.DetectedPhrase
import com.oneloneraven.deaftly.core.ocr.LiveRecognition
import com.oneloneraven.deaftly.core.ocr.Phrase
import com.oneloneraven.deaftly.core.ocr.PhraseTracker
import com.oneloneraven.deaftly.core.ocr.PixelFormat
import com.oneloneraven.deaftly.core.ocr.RecognitionFrame
import com.oneloneraven.deaftly.core.ocr.ViewportMapping
import com.oneloneraven.deaftly.ocr.bench.FixtureSet
import com.oneloneraven.deaftly.ocr.bench.OcrBenchmark
import com.oneloneraven.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 * Replays a hand-held camera over printed text through the live path (real engine, segmenter, filter and tracker) and
 * measures how the outlines behave: how many are shown against how many lines are detected, how many outlines overlap,
 * how many sit over nothing, how often a line gets a new identity, and how far outlines lag the text. Each tracker variant
 * sees the same detections, so the differences come from tracking alone. Diagnostic for #222.
 *
 * The recorded scene is 54 frames from a 10.8 s hand-held phone video of a cereal box nutrition table, taken at 5 fps and
 * cropped to the camera's 3:4 at 720 by 960.
 */
@RunWith(AndroidJUnit4::class)
class TrackingReplayTest {
    private class Scene(
        val name: String,
        val frames: () -> List<RecognitionFrame>,
    )

    private class Variant(
        val name: String,
        val tracker: () -> PhraseTracker,
    )

    private class Totals {
        var frames = 0
        var detected = 0
        var shown = 0
        var overlapping = 0
        var overNothing = 0
        var newIds = 0
        var lagSum = 0.0
        var lagCount = 0

        fun line(name: String): String =
            "%-14s shown %5.1f  detected %5.1f  overlapping pairs %5.1f  over nothing %4.1f  new ids/frame %4.1f  lag %5.1f px".format(
                name,
                shown.toDouble() / frames,
                detected.toDouble() / frames,
                overlapping.toDouble() / frames,
                overNothing.toDouble() / frames,
                newIds.toDouble() / (frames - 1),
                if (lagCount == 0) 0.0 else lagSum / lagCount,
            )
    }

    private fun synthetic(
        name: String,
        region: RectF?,
    ): Scene =
        Scene("$name (label-cereal, $FRAMES frames of seeded wobble)") {
            val source = checkNotNull(FixtureSet.load().firstOrNull { it.truth.id == "label-cereal" }) { "Missing label-cereal" }.bitmap
            wobble(Random(SEED), FRAMES).map { frameOf(source, region, it) }
        }

    private fun recorded(): Scene {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val names =
            assets
                .list(RECORDED_DIRECTORY)
                .orEmpty()
                .filter { it.endsWith(".jpg") }
                .sorted()
        check(names.isNotEmpty()) { "No recorded frames in $RECORDED_DIRECTORY" }
        return Scene("recorded (${names.size} frames of a nutrition table, hand-held phone)") {
            names.map { name ->
                val bitmap = assets.open("$RECORDED_DIRECTORY/$name").use { BitmapFactory.decodeStream(it) }
                frameOf(checkNotNull(bitmap) { "Cannot decode $name" })
            }
        }
    }

    private val scenes get() = listOf(synthetic("close", RectF(95f, 360f, 670f, 840f)), synthetic("far", null), recorded())

    private val variants =
        listOf(
            Variant("defaults") { PhraseTracker() },
            Variant("before #222") { PhraseTracker(minOverlap = 0.4f, smoothing = 0.6f, maxMisses = 2) },
            Variant("no stale") { PhraseTracker(maxMisses = 0) },
            Variant("no easing") { PhraseTracker(smoothing = 1f) },
        )

    @Test
    fun replaysAHandHeldCameraOverSmallDenseText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val report = StringBuilder()
        PaddleOcrNcnnRecogniser(context).use { recogniser ->
            val live = LiveRecognition(recogniser)
            scenes.forEach { scene ->
                val trackers = variants.map { it.tracker() }
                val totals = variants.map { Totals() }
                val seen = variants.map { mutableSetOf<Long>() }
                scene.frames().forEachIndexed { index, frame ->
                    val mapping = ViewportMapping.of(frame, BoundingBox(0f, 0f, FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat()))
                    val detected = live.detect(runBlocking { recogniser.recognise(frame) }, mapping)
                    trackers.forEachIndexed { v, tracker ->
                        val shown = tracker.update(detected)
                        record(totals[v], detected, shown, seen[v], firstFrame = index == 0)
                    }
                }
                report.appendLine(scene.name)
                variants.forEachIndexed { v, variant -> report.appendLine("  " + totals[v].line(variant.name)) }
            }
        }
        Log.i(TAG, "\n" + report)
        File(OcrBenchmark.outputDirectory().also { it.mkdirs() }, "tracking-replay.txt").writeText(report.toString())
        assertTrue(report.isNotEmpty())
    }

    private fun record(
        totals: Totals,
        detected: List<DetectedPhrase>,
        shown: List<Phrase>,
        seen: MutableSet<Long>,
        firstFrame: Boolean,
    ) {
        totals.frames++
        totals.detected += detected.size
        totals.shown += shown.size
        val boxes = shown.map { it.quad.boundingBox() }
        for (i in boxes.indices) for (j in i + 1 until boxes.size) if (iou(boxes[i], boxes[j]) > OVERLAP_PAIR) totals.overlapping++
        shown.forEach { phrase ->
            val box = phrase.quad.boundingBox()
            val best = detected.maxByOrNull { iou(box, it.quad.boundingBox()) }
            if (best == null || iou(box, best.quad.boundingBox()) < OVER_NOTHING) {
                totals.overNothing++
            } else {
                val target = best.quad.boundingBox()
                totals.lagSum +=
                    hypot(
                        (centreX(box) - centreX(target)) * FRAME_WIDTH,
                        (centreY(box) - centreY(target)) * FRAME_HEIGHT,
                    )
                totals.lagCount++
            }
            if (seen.add(phrase.id) && !firstFrame) totals.newIds++
        }
    }

    private class Jitter(
        val dx: Float,
        val dy: Float,
        val degrees: Float,
        val scale: Float,
    )

    private fun wobble(
        random: Random,
        count: Int,
    ): List<Jitter> {
        var dx = 0f
        var dy = 0f
        var degrees = 0f
        var scale = 1f
        return List(count) {
            dx = (dx + random.step(STEP_PX)).coerceIn(-MAX_PX, MAX_PX)
            dy = (dy + random.step(STEP_PX)).coerceIn(-MAX_PX, MAX_PX)
            degrees = (degrees + random.step(STEP_DEGREES)).coerceIn(-MAX_DEGREES, MAX_DEGREES)
            scale = (scale + random.step(STEP_SCALE)).coerceIn(1f - MAX_SCALE, 1f + MAX_SCALE)
            Jitter(dx, dy, degrees, scale)
        }
    }

    private fun Random.step(size: Float): Float = (nextFloat() * 2f - 1f) * size

    private fun frameOf(
        source: Bitmap,
        region: RectF?,
        jitter: Jitter,
    ): RecognitionFrame {
        val bitmap = Bitmap.createBitmap(FRAME_WIDTH, FRAME_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(BACKGROUND, BACKGROUND, BACKGROUND))
        val from = region ?: RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
        val to = RectF(0f, 0f, FRAME_WIDTH.toFloat(), FRAME_HEIGHT.toFloat())
        val matrix = Matrix().apply { setRectToRect(from, to, Matrix.ScaleToFit.CENTER) }
        matrix.postScale(jitter.scale, jitter.scale, FRAME_WIDTH / 2f, FRAME_HEIGHT / 2f)
        matrix.postRotate(jitter.degrees, FRAME_WIDTH / 2f, FRAME_HEIGHT / 2f)
        matrix.postTranslate(jitter.dx, jitter.dy)
        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return frameOf(bitmap)
    }

    private fun frameOf(bitmap: Bitmap): RecognitionFrame {
        val buffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()
        return RecognitionFrame(bitmap.width, bitmap.height, 0, PixelFormat.RGBA_8888, buffer, bitmap.rowBytes)
    }

    private fun iou(
        a: BoundingBox,
        b: BoundingBox,
    ): Float {
        val width = min(a.right, b.right) - max(a.left, b.left)
        val height = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (width <= 0f || height <= 0f) return 0f
        val intersection = width * height
        val union = a.width * a.height + b.width * b.height - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    private fun centreX(box: BoundingBox) = (box.left + box.right) / 2f

    private fun centreY(box: BoundingBox) = (box.top + box.bottom) / 2f

    private companion object {
        const val TAG = "TrackingReplay"
        const val FRAME_WIDTH = 720
        const val FRAME_HEIGHT = 960
        const val FRAMES = 40
        const val SEED = 222
        const val RECORDED_DIRECTORY = "ocr-replay/cereal-nutrition-handheld"
        const val BACKGROUND = 90
        const val STEP_PX = 6f
        const val MAX_PX = 30f
        const val STEP_DEGREES = 0.6f
        const val MAX_DEGREES = 3f
        const val STEP_SCALE = 0.015f
        const val MAX_SCALE = 0.06f
        const val OVERLAP_PAIR = 0.2f
        const val OVER_NOTHING = 0.1f
    }
}
