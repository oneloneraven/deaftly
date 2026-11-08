package com.ravendarque.deaftly.ocr.bench

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextRecogniser
import com.ravendarque.deaftly.core.ocr.bench.BenchReport
import com.ravendarque.deaftly.core.ocr.bench.ImageRun
import com.ravendarque.deaftly.core.ocr.bench.OcrScorer
import com.ravendarque.deaftly.core.ocr.bench.Stats
import kotlinx.coroutines.runBlocking
import java.io.File

class Candidate(
    val name: String,
    val settings: Map<String, String>,
    val create: () -> TextRecogniser,
    val stageTimings: (TextRecogniser) -> Map<String, Double> = { emptyMap() },
)

object OcrBenchmark {
    private const val TAG = "OcrBenchmark"
    private const val WARM_UPS = 1
    private const val TIMED_RUNS = 3
    private const val NANOS_PER_MILLI = 1_000_000.0
    private const val JPEG_QUALITY = 80
    private const val LINE_STROKE = 3f
    private const val WORD_STROKE = 2f

    fun run(
        candidate: Candidate,
        fixtures: List<Fixture>,
        outputDirectory: File,
        overlays: Boolean,
    ): BenchReport {
        outputDirectory.mkdirs()
        val stageSamples = mutableMapOf<String, MutableList<Double>>()
        val runs =
            candidate.create().use { recogniser ->
                fixtures.map { fixture ->
                    var result = RecognitionResult.EMPTY
                    repeat(WARM_UPS) { result = runBlocking { recogniser.recognise(fixture.frame) } }
                    val latencies =
                        List(TIMED_RUNS) {
                            val start = System.nanoTime()
                            result = runBlocking { recogniser.recognise(fixture.frame) }
                            (System.nanoTime() - start) / NANOS_PER_MILLI
                        }
                    candidate.stageTimings(recogniser).forEach { (stage, ms) -> stageSamples.getOrPut(stage) { mutableListOf() }.add(ms) }
                    if (overlays) writeOverlay(fixture, result, File(outputDirectory, "${candidate.name}-${fixture.truth.id}.jpg"))
                    ImageRun(OcrScorer.score(fixture.truth, result), latencies)
                }
            }
        val stages = stageSamples.entries.joinToString(", ") { "${it.key}=%.0f".format(Stats.median(it.value)) }
        val report = BenchReport(candidate.name, candidate.settings, environment(stages), runs)
        File(outputDirectory, "${candidate.name}.json").writeText(report.toJson())
        File(outputDirectory, "${candidate.name}.md").writeText(report.toMarkdown())
        Log.i(TAG, "Wrote ${candidate.name} to $outputDirectory")
        return report
    }

    fun outputDirectory(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val requested = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        return File(requested ?: checkNotNull(context.getExternalFilesDir(null)).path, "ocr-bench")
    }

    private fun environment(stageMedians: String): Map<String, String> =
        buildMap {
            put("Device", "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            put("ABIs", Build.SUPPORTED_ABIS.joinToString(","))
            put("CPU", cpuModel())
            put("Cores", Runtime.getRuntime().availableProcessors().toString())
            put("Median stage ms", stageMedians.ifEmpty { "n/a" })
            put("Peak RSS of test process (kB)", peakResidentKb().toString())
        }

    private fun cpuModel(): String =
        runCatching {
            File("/proc/cpuinfo")
                .readLines()
                .firstOrNull { it.startsWith("model name") || it.startsWith("Hardware") }
                ?.substringAfter(':')
                ?.trim()
        }.getOrNull() ?: "unknown"

    private fun peakResidentKb(): Long =
        runCatching {
            File("/proc/self/status")
                .readLines()
                .first { it.startsWith("VmHWM:") }
                .filter { it.isDigit() }
                .toLong()
        }.getOrDefault(-1L)

    private fun writeOverlay(
        fixture: Fixture,
        result: RecognitionResult,
        target: File,
    ) {
        val canvasBitmap = fixture.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(canvasBitmap)
        val linePaint =
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = LINE_STROKE
                color = Color.rgb(0, 200, 0)
            }
        val wordPaint =
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = WORD_STROKE
                color = Color.rgb(255, 80, 0)
            }
        for (line in result.blocks.flatMap { it.lines }) {
            line.corners?.points?.let { points ->
                for (i in points.indices) {
                    val next = points[(i + 1) % points.size]
                    canvas.drawLine(points[i].x, points[i].y, next.x, next.y, linePaint)
                }
            }
            for (word in line.words) {
                canvas.drawRect(word.boundingBox.left, word.boundingBox.top, word.boundingBox.right, word.boundingBox.bottom, wordPaint)
            }
        }
        target.outputStream().use { canvasBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        canvasBitmap.recycle()
    }
}
