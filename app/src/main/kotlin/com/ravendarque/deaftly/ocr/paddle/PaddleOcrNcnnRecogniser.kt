package com.ravendarque.deaftly.ocr.paddle

import android.content.Context
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.PixelFormat
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextBlock
import com.ravendarque.deaftly.core.ocr.TextLine
import com.ravendarque.deaftly.core.ocr.TextRecogniser
import com.ravendarque.deaftly.core.ocr.TextWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.atan2

enum class PaddleRecModel(
    val assetPrefix: String,
) {
    MULTILINGUAL("ocr/ppocrv5_multi"),
    LATIN("ocr/ppocrv5_latin"),
}

data class PaddleOcrConfig(
    val recModel: PaddleRecModel = PaddleRecModel.LATIN,
    val detectionSide: Int = DEFAULT_DETECTION_SIDE,
    val threads: Int = DEFAULT_THREADS,
    val fp16: Boolean = true,
) {
    val label: String get() = "PP-OCRv5 mobile ${recModel.name.lowercase()} rec, det ${detectionSide}px, $threads threads, fp16=$fp16"

    companion object {
        const val DEFAULT_DETECTION_SIDE = 960
        const val DEFAULT_THREADS = 4
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
                "${config.recModel.assetPrefix}_rec.param",
                "${config.recModel.assetPrefix}_rec.bin",
                "${config.recModel.assetPrefix}_dict.txt",
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
        return decode(native.geometry(handle), native.texts(handle), count)
    }

    private fun directCopy(frame: RecognitionFrame): ByteBuffer {
        val source = frame.pixels
        if (source.isDirect) return source
        val copy = ByteBuffer.allocateDirect(source.capacity())
        val view = source.duplicate()
        view.clear()
        copy.put(view)
        copy.clear()
        return copy
    }

    private fun decode(
        geometry: FloatArray,
        texts: Array<String>,
        lineCount: Int,
    ): RecognitionResult {
        var g = 0
        var t = 0
        val lines =
            List(lineCount) {
                val confidence = geometry[g++]
                val corners =
                    Quad(
                        List(QUAD_POINTS) {
                            Point(geometry[g++], geometry[g++])
                        },
                    )
                val wordCount = geometry[g++].toInt()
                val text = texts[t++]
                val words =
                    List(wordCount) {
                        val wordConfidence = geometry[g++]
                        val box = BoundingBox(geometry[g++], geometry[g++], geometry[g++], geometry[g++])
                        TextWord(texts[t++], box, wordConfidence)
                    }
                val (a, b) = corners.points
                TextLine(
                    text = text,
                    boundingBox = corners.boundingBox(),
                    words = words,
                    corners = corners,
                    angleDegrees = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat(),
                    confidence = confidence,
                )
            }
        return RecognitionResult(lines.map { TextBlock(listOf(it), it.boundingBox) })
    }

    override fun close() {
        synchronized(lock) {
            if (!closed) native.release(handle)
            closed = true
        }
    }

    private companion object {
        const val QUAD_POINTS = 4
        const val PREPARE = 0
        const val DETECT = 1
        const val POSTPROCESS = 2
        const val RECOGNISE = 3
    }
}
