package com.ravendarque.deaftly.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhraseSelectionTest {
    private fun phrase(
        id: Long,
        text: String,
        left: Float,
        top: Float,
    ): Phrase {
        val words = text.split(" ")
        val step = 0.1f
        return Phrase(
            id,
            text,
            BoundingBox(left, top, left + step * words.size, top + 0.05f).toQuad(),
            words.mapIndexed { i, w -> PhraseWord(w, BoundingBox(left + step * i, top, left + step * (i + 1), top + 0.05f).toQuad()) },
        )
    }

    private val first = phrase(1, "Pasta arrabbiata 12.50", 0.1f, 0.1f)
    private val second = phrase(2, "tomato, garlic", 0.1f, 0.2f)
    private val third = phrase(3, "Lasagne 14.00", 0.1f, 0.3f)
    private val phrases = listOf(first, second, third)

    @Test
    fun aWholePhraseSelectionYieldsThePhraseText() {
        assertEquals("tomato, garlic", PhraseSelection.text(phrases, PhraseSelection.wholePhrase(second)))
    }

    @Test
    fun aRangeWithinOnePhraseYieldsItsWords() {
        val selection = TextSelection(WordRef(1, 1), WordRef(1, 2))

        assertEquals("arrabbiata 12.50", PhraseSelection.text(phrases, selection))
    }

    @Test
    fun aRangeAcrossPhrasesRunsInReadingOrderWhicheverWayItWasDragged() {
        val forwards = TextSelection(WordRef(1, 2), WordRef(3, 0))
        val backwards = TextSelection(WordRef(3, 0), WordRef(1, 2))

        assertEquals("12.50 tomato, garlic Lasagne", PhraseSelection.text(phrases, forwards))
        assertEquals(PhraseSelection.text(phrases, forwards), PhraseSelection.text(phrases, backwards))
        assertEquals(listOf(1L, 2L, 3L), PhraseSelection.runs(phrases, forwards).map { it.phrase.id })
    }

    @Test
    fun aSelectionWhosePhraseHasGoneHasNoRunsOrText() {
        val selection = TextSelection(WordRef(9, 0), WordRef(9, 1))

        assertTrue(PhraseSelection.runs(phrases, selection).isEmpty())
        assertNull(PhraseSelection.text(phrases, selection))
    }

    @Test
    fun aTouchInsideAPhraseHitsIt() {
        assertEquals(1L, PhraseHitTest.phraseAt(phrases, Point(150f, 120f), 1000f, 1000f, slop = 0f)?.id)
    }

    @Test
    fun aTouchJustOutsideHitsTheNearestPhraseWithinTheSlop() {
        assertEquals(2L, PhraseHitTest.phraseAt(phrases, Point(150f, 270f), 1000f, 1000f, slop = 25f)?.id)
        assertNull(PhraseHitTest.phraseAt(phrases, Point(150f, 270f), 1000f, 1000f, slop = 10f))
    }

    @Test
    fun aTouchFarFromEveryPhraseHitsNothing() {
        assertNull(PhraseHitTest.phraseAt(phrases, Point(900f, 900f), 1000f, 1000f, slop = 30f))
    }

    @Test
    fun aRotatedPhraseIsHitWhereItIs() {
        val diamond =
            Phrase(
                7,
                "turned",
                Quad(listOf(Point(0.5f, 0.2f), Point(0.6f, 0.3f), Point(0.5f, 0.4f), Point(0.4f, 0.3f))),
                emptyList(),
            )

        assertEquals(7L, PhraseHitTest.phraseAt(listOf(diamond), Point(500f, 300f), 1000f, 1000f, slop = 0f)?.id)
        assertNull(PhraseHitTest.phraseAt(listOf(diamond), Point(580f, 220f), 1000f, 1000f, slop = 0f))
    }
}
