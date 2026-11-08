package com.ravendarque.deaftly.ocr.paddle

import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.core.ocr.RecognitionResult
import com.ravendarque.deaftly.core.ocr.TextBlock
import com.ravendarque.deaftly.core.ocr.TextLine
import com.ravendarque.deaftly.core.ocr.TextWord
import kotlin.math.atan2

internal object NativeResultDecoder {
    fun decode(
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
                        val wordCorners = Quad(List(QUAD_POINTS) { Point(geometry[g++], geometry[g++]) })
                        TextWord(texts[t++], wordCorners.boundingBox(), wordConfidence, wordCorners)
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

    private const val QUAD_POINTS = 4
}
