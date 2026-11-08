package com.ravendarque.deaftly.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.ocr.bench.Candidate
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.bench.OcrBenchmark
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrConfig
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import com.ravendarque.deaftly.ocr.paddle.PaddleRecModel
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PaddleOcrBenchmarkTest {
    private fun benchmark(
        name: String,
        config: PaddleOcrConfig,
    ) {
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
    }

    @Test
    fun latinRecogniser960() = benchmark("latin-det960", PaddleOcrConfig(PaddleRecModel.LATIN, 960, threads()))

    @Test
    fun multilingualRecogniser960() = benchmark("multi-det960", PaddleOcrConfig(PaddleRecModel.MULTILINGUAL, 960, threads()))

    @Test
    fun latinRecogniser1280() = benchmark("latin-det1280", PaddleOcrConfig(PaddleRecModel.LATIN, 1280, threads()))

    @Test
    fun latinRecogniser640() = benchmark("latin-det640", PaddleOcrConfig(PaddleRecModel.LATIN, 640, threads()))

    @Test
    fun latinRecogniser960SingleThread() = benchmark("latin-det960-1thread", PaddleOcrConfig(PaddleRecModel.LATIN, 960, 1))

    private fun threads() = Runtime.getRuntime().availableProcessors().coerceAtMost(PaddleOcrConfig.DEFAULT_THREADS)
}
