package com.ravendarque.deaftly.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OcrSelectionTest {
    private fun phrase(
        id: Long,
        text: String,
        top: Float,
    ): Phrase {
        val quad = BoundingBox(0.1f, top, 0.9f, top + 0.05f).toQuad()
        return Phrase(id, text, quad, text.split(" ").map { PhraseWord(it, quad) })
    }

    private val soup = phrase(1, "Soup of the day", 0.1f)
    private val bread = phrase(2, "Garlic bread", 0.2f)
    private val tea = phrase(3, "Pot of tea", 0.3f)
    private val phrases = listOf(soup, bread, tea)

    @Test
    fun nothingIsSelectedAtFirst() {
        val selection = OcrSelection()

        assertTrue(selection.isEmpty)
        assertEquals("", selection.tappedText)
        assertTrue(selection.runs(phrases).isEmpty())
        assertNull(selection.handleRange(phrases))
    }

    @Test
    fun tappingLinesAddsThemInTapOrderJoinedBySpaces() {
        val selection = OcrSelection().toggled(tea).toggled(soup)

        assertEquals("Pot of tea Soup of the day", selection.tappedText)
        assertTrue(selection.isTapped(tea.id))
        assertFalse(selection.isTapped(bread.id))
    }

    @Test
    fun tappingASelectedLineRemovesItAndKeepsTheOrderOfTheRest() {
        val selection =
            OcrSelection()
                .toggled(soup)
                .toggled(bread)
                .toggled(tea)
                .toggled(bread)

        assertEquals("Soup of the day Pot of tea", selection.tappedText)
        assertFalse(selection.isTapped(bread.id))
    }

    @Test
    fun removingTheLastLineLeavesNothingSelected() {
        val selection = OcrSelection().toggled(soup).toggled(soup)

        assertTrue(selection.isEmpty)
        assertEquals("", selection.tappedText)
    }

    @Test
    fun aRemovedLineGoesToTheEndWhenTappedAgain() {
        val selection =
            OcrSelection()
                .toggled(soup)
                .toggled(bread)
                .toggled(soup)
                .toggled(soup)

        assertEquals("Garlic bread Soup of the day", selection.tappedText)
    }

    @Test
    fun tappedLinesKeepTheirTextEvenIfTheLineLeavesTheView() {
        val selection = OcrSelection().toggled(soup).toggled(tea)

        assertEquals("Soup of the day Pot of tea", selection.tappedText)
        assertEquals(listOf(tea.id), selection.runs(listOf(tea)).map { it.phrase.id })
    }

    @Test
    fun tappedLinesAreHighlightedWhole() {
        val runs = OcrSelection().toggled(bread).toggled(soup).runs(phrases)

        assertEquals(listOf(soup.id, bread.id), runs.map { it.phrase.id })
        assertTrue(runs.all { it.coversPhrase })
    }

    @Test
    fun draggingReplacesTheTappedLines() {
        val range = TextSelection(WordRef(soup.id, 1), WordRef(bread.id, 0))

        val selection = OcrSelection().toggled(soup).toggled(tea).dragged(range)

        assertEquals(range, selection.range)
        assertTrue(selection.lines.isEmpty())
        assertEquals(listOf(soup.id, bread.id), selection.runs(phrases).map { it.phrase.id })
    }

    @Test
    fun tappingAfterADragStartsANewSelectionOfThatLine() {
        val dragged = OcrSelection().dragged(TextSelection(WordRef(soup.id, 0), WordRef(bread.id, 1)))

        val selection = dragged.toggled(tea)

        assertNull(selection.range)
        assertEquals("Pot of tea", selection.tappedText)
    }

    @Test
    fun aSingleTappedLineOffersEndHandlesForTheWholeLine() {
        val selection = OcrSelection().toggled(bread)

        assertEquals(PhraseSelection.wholePhrase(bread), selection.handleRange(phrases))
    }

    @Test
    fun severalTappedLinesOfferNoHandles() {
        assertNull(OcrSelection().toggled(bread).toggled(tea).handleRange(phrases))
    }

    @Test
    fun aDraggedRangeOffersItsOwnHandles() {
        val range = TextSelection(WordRef(soup.id, 1), WordRef(bread.id, 0))

        assertEquals(range, OcrSelection().dragged(range).handleRange(phrases))
    }
}
