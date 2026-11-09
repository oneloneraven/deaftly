package com.ravendarque.deaftly.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextLineFilterTest {
    private val area = BoundingBox(0f, 0f, 10f, 10f)

    private fun line(
        text: String,
        confidence: Float? = 0.9f,
    ) = TextLine(text, area, confidence = confidence)

    @Test
    fun ordinaryTextIsKept() {
        assertTrue(TextLineFilter.isText(line("Soup of the day")))
        assertTrue(TextLineFilter.isText(line("£4.50")))
        assertTrue(TextLineFilter.isText(line("Crème brûlée")))
    }

    @Test
    fun blankLinesAreDropped() {
        assertFalse(TextLineFilter.isText(line("")))
        assertFalse(TextLineFilter.isText(line(" \t ")))
    }

    @Test
    fun linesThatAreMostlySymbolsAreDropped() {
        assertFalse(TextLineFilter.isText(line("|")))
        assertFalse(TextLineFilter.isText(line("- - - -")))
        assertFalse(TextLineFilter.isText(line("..:;'")))
        assertFalse(TextLineFilter.isText(line("~|a|~|~")))
    }

    @Test
    fun aLineIsKeptWhenHalfItsCharactersAreLettersOrDigits() {
        assertTrue(TextLineFilter.isText(line("a-")))
        assertFalse(TextLineFilter.isText(line("a--")))
    }

    @Test
    fun lowConfidenceLinesAreDropped() {
        assertFalse(TextLineFilter.isText(line("Soup", confidence = 0.2f)))
        assertTrue(TextLineFilter.isText(line("Soup", confidence = TextLineFilter.DEFAULT_MIN_CONFIDENCE)))
    }

    @Test
    fun theMinimumConfidenceCanBeChanged() {
        assertTrue(TextLineFilter.isText(line("Soup", confidence = 0.2f), minConfidence = 0.1f))
        assertFalse(TextLineFilter.isText(line("Soup", confidence = 0.8f), minConfidence = 0.9f))
    }

    @Test
    fun aLineWithoutAConfidenceIsJudgedOnItsTextAlone() {
        assertTrue(TextLineFilter.isText(line("Soup", confidence = null)))
        assertFalse(TextLineFilter.isText(line("|||", confidence = null)))
    }

    @Test
    fun filteringKeepsRealLinesAndDropsEmptyBlocks() {
        val soup = line("Soup")
        val noise = line("|", confidence = 0.3f)
        val result =
            RecognitionResult(
                listOf(TextBlock(listOf(soup, noise), area), TextBlock(listOf(noise), area)),
            )

        val filtered = TextLineFilter.filter(result)

        assertEquals(listOf(listOf(soup)), filtered.blocks.map { it.lines })
    }
}
