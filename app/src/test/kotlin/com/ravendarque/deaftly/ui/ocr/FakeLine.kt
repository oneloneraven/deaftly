package com.ravendarque.deaftly.ui.ocr

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
