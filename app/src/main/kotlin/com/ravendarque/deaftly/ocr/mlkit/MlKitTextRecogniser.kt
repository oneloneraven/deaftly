package com.ravendarque.deaftly.ocr.mlkit

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.atan2
import android.graphics.Point as AndroidPoint

class MlKitTextRecogniser : TextRecogniser {
    private val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognise(frame: RecognitionFrame): RecognitionResult {
        val text = process(frame)
        return RecognitionResult(text.textBlocks.map(::toBlock))
    }

    private suspend fun process(frame: RecognitionFrame): Text {
        val pixels = frame.pixels.duplicate().apply { clear() }
        val image =
            when (frame.format) {
                PixelFormat.RGBA_8888 -> {
                    InputImage.fromBitmap(toBitmap(frame, pixels), frame.rotationDegrees)
                }

                PixelFormat.NV21 -> {
                    InputImage.fromByteBuffer(pixels, frame.width, frame.height, frame.rotationDegrees, InputImage.IMAGE_FORMAT_NV21)
                }
            }
        return suspendCancellableCoroutine { continuation ->
            recogniser
                .process(image)
                .addOnSuccessListener(DIRECT) { continuation.resume(it) }
                .addOnFailureListener(DIRECT) { continuation.resumeWithException(it) }
        }
    }

    @SuppressLint("UseKtx")
    private fun toBitmap(
        frame: RecognitionFrame,
        pixels: ByteBuffer,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        if (frame.rowStrideBytes == frame.width * BYTES_PER_PIXEL) {
            bitmap.copyPixelsFromBuffer(pixels)
            return bitmap
        }
        val row = ByteArray(frame.width * BYTES_PER_PIXEL)
        val packed = ByteBuffer.allocateDirect(row.size * frame.height)
        for (y in 0 until frame.height) {
            pixels.position(y * frame.rowStrideBytes)
            pixels.get(row)
            packed.put(row)
        }
        packed.rewind()
        bitmap.copyPixelsFromBuffer(packed)
        return bitmap
    }

    private fun toBlock(block: Text.TextBlock): TextBlock {
        val lines = block.lines.map(::toLine)
        val box = block.boundingBox?.let(::toBox) ?: lines.map { it.boundingBox }.union()
        return TextBlock(lines, box)
    }

    private fun toLine(line: Text.Line): TextLine {
        val corners = line.cornerPoints?.takeIf { it.size == QUAD_POINTS }?.let(::toQuad)
        val box = corners?.boundingBox() ?: line.boundingBox?.let(::toBox) ?: EMPTY_BOX
        val angle =
            corners?.points?.let { (a, b) -> Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat() }
        return TextLine(
            text = line.text,
            boundingBox = box,
            words = line.elements.map(::toWord),
            corners = corners,
            angleDegrees = angle,
            confidence = line.confidence,
        )
    }

    private fun toWord(element: Text.Element): TextWord {
        val box =
            element.cornerPoints
                ?.takeIf { it.size == QUAD_POINTS }
                ?.let(::toQuad)
                ?.boundingBox()
                ?: element.boundingBox?.let(::toBox)
                ?: EMPTY_BOX
        return TextWord(element.text, box, element.confidence)
    }

    private fun toQuad(points: Array<AndroidPoint>): Quad = Quad(points.map { Point(it.x.toFloat(), it.y.toFloat()) })

    private fun toBox(rect: Rect): BoundingBox =
        BoundingBox(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())

    private fun List<BoundingBox>.union(): BoundingBox =
        if (isEmpty()) {
            EMPTY_BOX
        } else {
            BoundingBox(minOf { it.left }, minOf { it.top }, maxOf { it.right }, maxOf { it.bottom })
        }

    override fun close() {
        recogniser.close()
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4
        const val QUAD_POINTS = 4
        val EMPTY_BOX = BoundingBox(0f, 0f, 0f, 0f)
        val DIRECT = Executor { it.run() }
    }
}
