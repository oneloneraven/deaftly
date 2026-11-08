package com.ravendarque.deaftly.ui.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.PhraseWord

class FakeLine(
    val text: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

val fakeMenu =
    listOf(
        FakeLine("PASTA", 0.35f, 0.175f, 0.30f, 0.035f),
        FakeLine("Pasta arrabbiata   12.50", 0.12f, 0.235f, 0.76f, 0.035f),
        FakeLine("tomato, garlic, chilli", 0.12f, 0.285f, 0.50f, 0.028f),
        FakeLine("Spaghetti carbonara   13.00", 0.12f, 0.335f, 0.76f, 0.035f),
        FakeLine("Penne al pesto   11.50", 0.12f, 0.385f, 0.76f, 0.035f),
        FakeLine("Lasagne   14.00", 0.12f, 0.435f, 0.76f, 0.035f),
        FakeLine("SIDES", 0.38f, 0.495f, 0.24f, 0.035f),
        FakeLine("Garlic bread   4.50", 0.12f, 0.545f, 0.76f, 0.035f),
    )

fun FakeLine.toPhrase(id: Long): Phrase {
    val box = BoundingBox(left, top, left + width, top + height)
    val words =
        Regex("\\S+").findAll(text).map { match ->
            val start = left + width * match.range.first / text.length
            val end = left + width * (match.range.last + 1) / text.length
            PhraseWord(match.value, BoundingBox(start, top, end, top + height).toQuad())
        }
    return Phrase(id, text.replace(Regex("\\s+"), " "), box.toQuad(), words.toList())
}

val menuPhrases: List<Phrase> = fakeMenu.mapIndexed { index, line -> line.toPhrase(index + 1L) }

fun fakeFrameImage(
    width: Int = 780,
    height: Int = 1688,
): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFF1F1A15.toInt())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0xFFF3ECDC.toInt()
    canvas.drawRect(width * 0.08f, height * 0.14f, width * 0.92f, height * 0.70f, paint)
    paint.color = 0xFF2A2018.toInt()
    paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    fakeMenu.forEach { line ->
        val lineWidth = width * line.width
        paint.textSize = minOf(lineWidth / line.text.length / 0.6f, height * line.height)
        canvas.drawText(line.text, width * line.left, height * line.top + paint.textSize * 0.85f, paint)
    }
    return bitmap.asImageBitmap()
}
