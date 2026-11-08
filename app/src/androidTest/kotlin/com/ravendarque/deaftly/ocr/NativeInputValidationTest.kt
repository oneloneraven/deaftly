package com.ravendarque.deaftly.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.ocr.paddle.NativePaddleOcr
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NativeInputValidationTest {
    private val native = NativePaddleOcr()
    private var handle = 0L

    @Before
    fun load() {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        handle =
            native.create(
                assets,
                "ocr/ppocrv5_mobile_det.param",
                "ocr/ppocrv5_mobile_det.bin",
                "ocr/ppocrv5_latin_rec.param",
                "ocr/ppocrv5_latin_rec.bin",
                "ocr/ppocrv5_latin_dict.txt",
                SIDE,
                1,
                true,
            )
        assertTrue(handle != 0L)
    }

    @After
    fun release() = native.release(handle)

    @Test
    fun rejectsInconsistentGeometryWithoutReadingTheBuffer() {
        val buffer = ByteBuffer.allocateDirect(SMALL * SMALL * 4)
        val invalid =
            listOf(
                Geometry(0, SMALL, SMALL * 4, 0),
                Geometry(SMALL, 0, SMALL * 4, 0),
                Geometry(-1, SMALL, SMALL * 4, 0),
                Geometry(SMALL, SMALL, SMALL * 4 - 1, 0),
                Geometry(SMALL, SMALL, SMALL * 4, 45),
                Geometry(SMALL, SMALL + 1, SMALL * 4, 0),
                Geometry(SMALL, SMALL, Int.MAX_VALUE, 0),
                Geometry(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, 0),
                Geometry(MAX_SIDE + 1, 1, (MAX_SIDE + 1) * 4, 0),
                Geometry(MAX_SIDE, MAX_SIDE, MAX_SIDE * 4, 0),
            )
        for (g in invalid) {
            assertEquals(-1, native.recognise(handle, buffer, g.width, g.height, g.stride, g.rotation), "$g")
        }
    }

    @Test
    fun rejectsABufferThatIsNotDirect() {
        val heap = ByteBuffer.allocate(SMALL * SMALL * 4)
        assertEquals(-1, native.recognise(handle, heap, SMALL, SMALL, SMALL * 4, 0))
    }

    @Test
    fun survivesRandomNoiseAndDegenerateFrames() {
        val random = Random(SEED)
        PaddleOcrNcnnRecogniser(InstrumentationRegistry.getInstrumentation().targetContext).use { recogniser ->
            val shapes = listOf(NOISE to NOISE, 1 to 1, 1 to NOISE, NOISE to 1, 31 to 33)
            for ((width, height) in shapes) {
                for (rotation in listOf(0, 90, 180, 270)) {
                    val bytes = ByteArray(width * height * 4).also(random::nextBytes)
                    val pixels = ByteBuffer.allocateDirect(bytes.size).also { it.put(bytes) }.also { it.rewind() }
                    val frame = RecognitionFrame(width, height, rotation, PixelFormat.RGBA_8888, pixels)
                    runBlocking { recogniser.recognise(frame) }
                }
            }
            val flat = ByteBuffer.allocateDirect(NOISE * NOISE * 4)
            runBlocking { recogniser.recognise(RecognitionFrame(NOISE, NOISE, 0, PixelFormat.RGBA_8888, flat)) }
        }
    }

    private data class Geometry(
        val width: Int,
        val height: Int,
        val stride: Int,
        val rotation: Int,
    )

    private companion object {
        const val SIDE = 960
        const val SMALL = 8
        const val NOISE = 320
        const val MAX_SIDE = 16384
        const val SEED = 26
    }
}
