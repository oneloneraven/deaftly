package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FormattedTextTest {
    private val bold = setOf(SpanStyle.Bold)

    private fun hello() = FormattedText("Hello brave world", listOf(StyleRange(6, 11, SpanStyle.Bold)))

    @Test
    fun spansSplitTheTextAtTheRanges() {
        assertEquals(listOf(Span("Hello "), Span("brave", bold), Span(" world")), hello().spans)
    }

    @Test
    fun aRangeBeyondTheTextIsRejected() {
        assertFailsWith<IllegalArgumentException> { FormattedText("abc", listOf(StyleRange(1, 4, SpanStyle.Bold))) }
    }

    @Test
    fun insertingBeforeARangeShiftsIt() {
        val result = hello().replace(0, 0, ">> ")
        assertEquals(">> Hello brave world", result.text)
        assertEquals(listOf(StyleRange(9, 14, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun insertingAfterARangeLeavesIt() {
        val result = hello().replace(17, 17, "!")
        assertEquals(listOf(StyleRange(6, 11, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun insertingInsideARangeWithoutTheStyleSplitsIt() {
        val result = hello().replace(8, 8, "XY")
        assertEquals("Hello brXYave world", result.text)
        assertEquals(listOf(StyleRange(6, 8, SpanStyle.Bold), StyleRange(10, 13, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun insertingInsideARangeWithTheStyleExtendsIt() {
        val result = hello().replace(8, 8, "XY", bold)
        assertEquals(listOf(StyleRange(6, 13, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun insertingWithAStyleAtTheEndOfARangeMergesWithIt() {
        val result = hello().replace(11, 11, "!", bold)
        assertEquals(listOf(StyleRange(6, 12, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun deletingTheWholeRangeRemovesIt() {
        val result = hello().replace(6, 11, "")
        assertEquals("Hello  world", result.text)
        assertTrue(result.ranges.isEmpty())
    }

    @Test
    fun deletingAcrossTheStartOfARangeTrimsIt() {
        val result = hello().replace(4, 8, "")
        assertEquals("Hellave world", result.text)
        assertEquals(listOf(StyleRange(4, 7, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun replacingARangeWithPlainTextDropsTheStyle() {
        val result = hello().replace(6, 11, "tall")
        assertTrue(result.ranges.isEmpty())
    }

    @Test
    fun replaceOutsideTheTextIsRejected() {
        assertFailsWith<IllegalArgumentException> { hello().replace(0, 99, "") }
        assertFailsWith<IllegalArgumentException> { hello().replace(5, 2, "") }
    }

    @Test
    fun togglingAnUnstyledSelectionAddsTheStyle() {
        val result = FormattedText("Hello").toggle(SpanStyle.Italic, 1, 4)
        assertEquals(listOf(StyleRange(1, 4, SpanStyle.Italic)), result.ranges)
    }

    @Test
    fun togglingAFullyStyledSelectionRemovesTheStyleFromJustThatPart() {
        val result = hello().toggle(SpanStyle.Bold, 7, 9)
        assertEquals(listOf(StyleRange(6, 7, SpanStyle.Bold), StyleRange(9, 11, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun togglingAPartlyStyledSelectionStylesTheWholeSelection() {
        val result = hello().toggle(SpanStyle.Bold, 4, 8)
        assertEquals(listOf(StyleRange(4, 11, SpanStyle.Bold)), result.ranges)
    }

    @Test
    fun togglingBridgesNeighbouringRanges() {
        val text = FormattedText("abcdef", listOf(StyleRange(0, 2, SpanStyle.Bold), StyleRange(4, 6, SpanStyle.Bold)))
        assertEquals(listOf(StyleRange(0, 6, SpanStyle.Bold)), text.toggle(SpanStyle.Bold, 1, 5).ranges)
    }

    @Test
    fun togglingOneStyleLeavesTheOthersAlone() {
        val text = FormattedText("abc", listOf(StyleRange(0, 3, SpanStyle.Bold)))
        assertEquals(
            setOf(StyleRange(0, 3, SpanStyle.Bold), StyleRange(1, 2, SpanStyle.Highlight)),
            text.toggle(SpanStyle.Highlight, 1, 2).ranges.toSet(),
        )
    }

    @Test
    fun togglingAnEmptySelectionChangesNothing() {
        val text = hello()
        assertSame(text, text.toggle(SpanStyle.Bold, 3, 3))
    }

    @Test
    fun isStyledNeedsTheWholeSelectionCovered() {
        assertTrue(hello().isStyled(SpanStyle.Bold, 6, 11))
        assertTrue(hello().isStyled(SpanStyle.Bold, 7, 9))
        assertFalse(hello().isStyled(SpanStyle.Bold, 5, 9))
        assertFalse(hello().isStyled(SpanStyle.Italic, 6, 11))
        assertFalse(hello().isStyled(SpanStyle.Bold, 7, 7))
    }

    @Test
    fun stylesBeforeAnIndexAreThoseOfThePrecedingCharacter() {
        assertEquals(bold, hello().stylesBefore(11))
        assertEquals(bold, hello().stylesBefore(7))
        assertTrue(hello().stylesBefore(6).isEmpty())
        assertTrue(hello().stylesBefore(0).isEmpty())
    }

    @Test
    fun trimmedDropsOuterWhitespaceAndKeepsTheRanges() {
        val text = FormattedText("  Hi there \n", listOf(StyleRange(2, 4, SpanStyle.Bold), StyleRange(0, 12, SpanStyle.Italic)))
        val result = text.trimmed()
        assertEquals("Hi there", result.text)
        assertEquals(setOf(StyleRange(0, 2, SpanStyle.Bold), StyleRange(0, 8, SpanStyle.Italic)), result.ranges.toSet())
    }

    @Test
    fun blankTextIsBlank() {
        assertTrue(FormattedText(" \n").isBlank)
        assertFalse(hello().isBlank)
    }
}
