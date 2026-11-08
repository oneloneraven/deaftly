package com.ravendarque.deaftly.ocr.paddle

import android.content.Context
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextRecogniser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * [detectionSide] is the longest side, in pixels, that the frame is scaled down to before text
 * detection. 960 is the default because it is the cheapest size that keeps most lines on the
 * fixture set (about 10% word-bag CER against about 6% at 1280, at about three quarters of the latency);
 * raise it for accuracy on dense or small text, lower it for speed on the live view.
 */
data class PaddleOcrConfig(
    val detectionSide: Int = DEFAULT_DETECTION_SIDE,
    val threads: Int = DEFAULT_THREADS,
    val fp16: Boolean = true,
) {
    init {
        require(detectionSide in MIN_DETECTION_SIDE..MAX_DETECTION_SIDE) { "Detection side out of range" }
        require(threads in 1..MAX_THREADS) { "Thread count out of range" }
    }

    val label: String get() = "PP-OCRv5 mobile latin rec, det ${detectionSide}px, $threads threads, fp16=$fp16"

    companion object {
        const val DEFAULT_DETECTION_SIDE = 960
        const val DEFAULT_THREADS = 4
        const val MIN_DETECTION_SIDE = 32
        const val MAX_DETECTION_SIDE = 4096
        const val MAX_THREADS = 16
    }
}

class PaddleOcrNcnnRecogniser(
    context: Context,
    val config: PaddleOcrConfig = PaddleOcrConfig(),
) : TextRecogniser {
    private val native = NativePaddleOcr()
    private val handle: Long
    private val lock = Any()
    private var stageTimings: Map<String, Double> = emptyMap()
    private var closed = false

    init {
        handle =
            native.create(
                context.assets,
                "ocr/ppocrv5_mobile_det.param",
                "ocr/ppocrv5_mobile_det.bin",
                "ocr/ppocrv5_latin_rec.param",
                "ocr/ppocrv5_latin_rec.bin",
                "ocr/ppocrv5_latin_dict.txt",
                config.detectionSide,
                config.threads,
                config.fp16,
            )
        check(handle != 0L) { "Could not load the PP-OCRv5 models" }
    }

    fun lastStageTimingsMs(): Map<String, Double> = synchronized(lock) { stageTimings }

    override suspend fun recognise(frame: RecognitionFrame): RecognitionResult {
        require(frame.format == PixelFormat.RGBA_8888) { "Only RGBA_8888 frames are supported" }
        return withContext(Dispatchers.Default) { synchronized(lock) { recogniseBlocking(frame) } }
    }

    private fun recogniseBlocking(frame: RecognitionFrame): RecognitionResult {
        check(!closed) { "Recogniser is closed" }
        val pixels = directCopy(frame)
        val count =
            native.recognise(handle, pixels, frame.width, frame.height, frame.rowStrideBytes, frame.rotationDegrees)
        check(count >= 0) { "Recognition failed" }
        val timings = native.timings(handle)
        stageTimings =
            mapOf(
                "prepare" to timings[PREPARE],
                "detect" to timings[DETECT],
                "postprocess" to timings[POSTPROCESS],
                "recognise" to timings[RECOGNISE],
            )
        return NativeResultDecoder.decode(native.geometry(handle), native.texts(handle), count)
    }

    private fun directCopy(frame: RecognitionFrame): ByteBuffer {
        val source = frame.pixels
        if (source.isDirect && source.position() == 0) return source
        val copy = ByteBuffer.allocateDirect(source.capacity())
        val view = source.duplicate()
        view.clear()
        copy.put(view)
        copy.clear()
        return copy
    }

    override fun close() {
        synchronized(lock) {
            if (!closed) native.release(handle)
            closed = true
        }
    }

    private companion object {
        const val PREPARE = 0
        const val DETECT = 1
        const val POSTPROCESS = 2
        const val RECOGNISE = 3
    }
}
