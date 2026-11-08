package com.ravendarque.deaftly.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.core.ocr.bench.BenchReport
import com.ravendarque.deaftly.ocr.bench.Candidate
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.bench.OcrBenchmark
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrConfig
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Regression gates on the generated fixture set (docs/spikes/25-paddleocr-ncnn/results.md). The limits sit just
 * above the measured word-bag character error rate (9.8% at 960, 5.8% at 1280) and just below the line recall
 * (81.8% and 92.2%). The detection-size sweep only runs with
 * `-Pandroid.testInstrumentationRunnerArguments.ocrBenchFull=true`.
 */
@RunWith(AndroidJUnit4::class)
class PaddleOcrBenchmarkTest {
    private fun benchmark(
        name: String,
        config: PaddleOcrConfig,
    ): BenchReport {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val candidate =
            Candidate(
                name = name,
                settings = mapOf("engine" to "PaddleOCR PP-OCRv5 mobile on ncnn 20261020", "config" to config.label),
                create = { PaddleOcrNcnnRecogniser(context, config) },
                stageTimings = { (it as PaddleOcrNcnnRecogniser).lastStageTimingsMs() },
            )
        val report = OcrBenchmark.run(candidate, FixtureSet.load(), OcrBenchmark.outputDirectory(), overlays = true)
        assertTrue(report.runs.isNotEmpty())
        return report
    }

    private fun assertWithin(
        report: BenchReport,
        maxCer: Double,
        minLineRecall: Double,
    ) {
        val total = report.aggregate()
        val cer = total.bag.charCaseSensitive.rate
        assertTrue(cer <= maxCer, "Word-bag CER ${"%.1f".format(cer * PERCENT)}% exceeds ${maxCer * PERCENT}%")
        assertTrue(
            total.lineRecall >= minLineRecall,
            "Line recall ${"%.1f".format(total.lineRecall * PERCENT)}% is below ${minLineRecall * PERCENT}%",
        )
    }

    @Test
    fun latinRecogniser960() = assertWithin(benchmark("latin-det960", PaddleOcrConfig(960, threads())), MAX_CER_960, MIN_RECALL_960)

    @Test
    fun latinRecogniser1280() = assertWithin(benchmark("latin-det1280", PaddleOcrConfig(1280, threads())), MAX_CER_1280, MIN_RECALL_1280)

    @Test
    fun latinRecogniser640() {
        assumeFullSweep()
        benchmark("latin-det640", PaddleOcrConfig(640, threads()))
    }

    @Test
    fun latinRecogniser960SingleThread() {
        assumeFullSweep()
        benchmark("latin-det960-1thread", PaddleOcrConfig(960, 1))
    }

    private fun assumeFullSweep() = assumeTrue(InstrumentationRegistry.getArguments().getString("ocrBenchFull") == "true")

    private fun threads() = Runtime.getRuntime().availableProcessors().coerceAtMost(PaddleOcrConfig.DEFAULT_THREADS)

    private companion object {
        const val PERCENT = 100.0
        const val MAX_CER_960 = 0.115
        const val MIN_RECALL_960 = 0.78
        const val MAX_CER_1280 = 0.07
        const val MIN_RECALL_1280 = 0.88
    }
}
