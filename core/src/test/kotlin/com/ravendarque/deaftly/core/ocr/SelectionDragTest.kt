package com.ravendarque.deaftly.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectionDragTest {
    private val geometry = DragGeometry(width = 1000f, height = 1000f, startSlop = 20f, handleRadius = 30f)

    private fun phrase(
        id: Long,
        text: String,
        top: Float,
    ): Phrase {
        val words = text.split(" ")
        return Phrase(
            id,
            text,
            BoundingBox(0.1f, top, 0.1f + 0.1f * words.size, top + 0.05f).toQuad(),
            words.mapIndexed { i, w -> PhraseWord(w, BoundingBox(0.1f + 0.1f * i, top, 0.2f + 0.1f * i, top + 0.05f).toQuad()) },
        )
    }

    private val phrases = listOf(phrase(1, "one two three", 0.1f), phrase(2, "four five", 0.2f), phrase(3, "six seven", 0.3f))

    private fun at(
        phrase: Int,
        word: Int,
    ): Point {
        val box = phrases[phrase].words[word].quad.boundingBox()
        return Point((box.left + box.right) / 2 * 1000f, (box.top + box.bottom) / 2 * 1000f)
    }

    @Test
    fun aDragStartingOnAWordStartsARangeAtThatWord() {
        val begun = SelectionDrag.begin(phrases, null, at(0, 1), geometry)

        assertEquals(TextSelection(WordRef(1, 1), WordRef(1, 1)), begun)
    }

    @Test
    fun aDragStartingAwayFromEveryWordDoesNotStart() {
        assertNull(SelectionDrag.begin(phrases, null, Point(900f, 900f), geometry))
    }

    @Test
    fun movingTheFingerExtendsTheRangeToTheNearestWordAcrossLines() {
        val begun = checkNotNull(SelectionDrag.begin(phrases, null, at(0, 1), geometry))

        val moved = SelectionDrag.move(phrases, begun, at(2, 0), geometry)

        assertEquals("two three four five six", PhraseSelection.text(phrases, moved))
    }

    @Test
    fun theFingerBetweenLinesStillPicksTheNearestWord() {
        val begun = checkNotNull(SelectionDrag.begin(phrases, null, at(0, 0), geometry))

        val moved = SelectionDrag.move(phrases, begun, Point(at(1, 1).x, 185f), geometry)

        assertEquals(WordRef(2, 1), moved.focus)
    }

    @Test
    fun draggingTheEndHandleKeepsTheStartAndMovesTheEnd() {
        val current = TextSelection(WordRef(1, 1), WordRef(2, 0))
        val endHandle = phrases[1].words[0].quad.points[2]
        val touch = Point(endHandle.x * 1000f + 5f, endHandle.y * 1000f)

        val begun = checkNotNull(SelectionDrag.begin(phrases, current, touch, geometry))
        val moved = SelectionDrag.move(phrases, begun, at(2, 1), geometry)

        assertEquals(WordRef(1, 1), moved.anchor)
        assertEquals("two three four five six seven", PhraseSelection.text(phrases, moved))
    }

    @Test
    fun draggingTheStartHandleKeepsTheEndAndMovesTheStart() {
        val current = TextSelection(WordRef(1, 1), WordRef(2, 1))
        val startHandle = phrases[0].words[1].quad.points[0]
        val touch = Point(startHandle.x * 1000f, startHandle.y * 1000f - 5f)

        val begun = checkNotNull(SelectionDrag.begin(phrases, current, touch, geometry))
        val moved = SelectionDrag.move(phrases, begun, at(0, 0), geometry)

        assertEquals(WordRef(2, 1), moved.anchor)
        assertEquals("one two three four five", PhraseSelection.text(phrases, moved))
    }
}
