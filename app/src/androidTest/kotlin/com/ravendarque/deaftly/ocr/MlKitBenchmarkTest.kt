package com.ravendarque.deaftly.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.bench.OrderMode
import com.ravendarque.deaftly.ocr.bench.Candidate
import com.ravendarque.deaftly.ocr.bench.Fixture
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.bench.OcrBenchmark
import com.ravendarque.deaftly.ocr.mlkit.MlKitTextRecogniser
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MlKitBenchmarkTest {
    private fun benchmark(
        name: String,
        input: String,
        order: OrderMode,
        frameOf: (Fixture) -> RecognitionFrame,
    ) {
        val candidate =
            Candidate(
                name = name,
                settings = mapOf("engine" to "ML Kit Text Recognition v2 16.0.1, bundled Latin", "input" to input, "order" to order.name),
                create = { MlKitTextRecogniser() },
                frameOf = frameOf,
                order = order,
            )
        val report = OcrBenchmark.run(candidate, FixtureSet.load(), OcrBenchmark.outputDirectory(), overlays = true)
        assertTrue(report.runs.isNotEmpty())
    }

    @Test
    fun rgbaBitmapNativeOrder() = benchmark("mlkit-rgba-native", "RGBA_8888 via Bitmap", OrderMode.NATIVE) { it.frame }

    @Test
    fun rgbaBitmapXyCutOrder() = benchmark("mlkit-rgba-xycut", "RGBA_8888 via Bitmap", OrderMode.XY_CUT) { it.frame }

    @Test
    fun nv21NativeOrder() = benchmark("mlkit-nv21-native", "NV21 via InputImage.fromByteBuffer", OrderMode.NATIVE) { it.nv21Frame }
}
