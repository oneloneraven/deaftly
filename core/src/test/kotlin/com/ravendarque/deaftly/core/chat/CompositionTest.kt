package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CompositionTest {
    private fun typed(
        composition: Composition,
        text: String,
    ) = composition.withText(text, text.length, text.length)

    @Test
    fun startsEmptyWithTheCursorAtTheEnd() {
        val composition = Composition(FormattedText("Hi"))
        assertEquals(2, composition.selectionStart)
        assertEquals(2, composition.selectionEnd)
        assertFalse(composition.hasSelection)
    }

    @Test
    fun aSelectionBeyondTheTextIsRejected() {
        assertFailsWith<IllegalArgumentException> { Composition(FormattedText("Hi"), 1, 3) }
    }

    @Test
    fun togglingAStyleWithACursorAppliesToTheNextTypedText() {
        val composition = typed(Composition().toggle(SpanStyle.Bold), "a")
        assertEquals(listOf(StyleRange(0, 1, SpanStyle.Bold)), composition.content.ranges)
        assertNull(composition.typing)
    }

    @Test
    fun theStyleCarriesOnToFurtherTyping() {
        val composition = typed(typed(Composition().toggle(SpanStyle.Italic), "a"), "ab")
        assertEquals(listOf(StyleRange(0, 2, SpanStyle.Italic)), composition.content.ranges)
    }

    @Test
    fun togglingTheStyleOffEndsItForTheNextTypedText() {
        val bold = typed(Composition().toggle(SpanStyle.Bold), "ab")
        val plain = typed(bold.toggle(SpanStyle.Bold), "abc")
        assertEquals(listOf(StyleRange(0, 2, SpanStyle.Bold)), plain.content.ranges)
    }

    @Test
    fun theCursorInheritsTheStyleOfThePrecedingText() {
        val composition = Composition(FormattedText("ab", listOf(StyleRange(0, 2, SpanStyle.Bold))))
        assertTrue(composition.isActive(SpanStyle.Bold))
        assertFalse(composition.isActive(SpanStyle.Italic))
        assertFalse(composition.select(0, 0).isActive(SpanStyle.Bold))
    }

    @Test
    fun aPendingStyleShowsAsActiveAndIsClearedByMovingTheCursor() {
        val composition = Composition(FormattedText("ab")).toggle(SpanStyle.Highlight)
        assertTrue(composition.isActive(SpanStyle.Highlight))
        val moved = composition.select(1, 1)
        assertFalse(moved.isActive(SpanStyle.Highlight))
        assertNull(moved.typing)
    }

    @Test
    fun selectingTheSameRangeKeepsThePendingStyle() {
        val composition = Composition(FormattedText("ab")).toggle(SpanStyle.Bold)
        assertSame(composition, composition.select(2, 2))
    }

    @Test
    fun togglingWithASelectionStylesTheSelectionNow() {
        val composition = Composition(FormattedText("Hello world")).select(6, 11).toggle(SpanStyle.Bold)
        assertEquals(listOf(StyleRange(6, 11, SpanStyle.Bold)), composition.content.ranges)
        assertTrue(composition.isActive(SpanStyle.Bold))
        assertNull(composition.typing)
    }

    @Test
    fun togglingAgainRemovesTheStyleFromTheSelection() {
        val composition = Composition(FormattedText("Hello world")).select(6, 11).toggle(SpanStyle.Bold).toggle(SpanStyle.Bold)
        assertTrue(composition.content.ranges.isEmpty())
        assertFalse(composition.isActive(SpanStyle.Bold))
    }

    @Test
    fun aPartlyStyledSelectionIsNotActive() {
        val composition = Composition(FormattedText("Hello world", listOf(StyleRange(0, 5, SpanStyle.Bold)))).select(3, 8)
        assertFalse(composition.isActive(SpanStyle.Bold))
    }

    @Test
    fun withTextTracksTheCursorAndSelectionItIsGiven() {
        val composition = Composition(FormattedText("abc")).withText("abXc", 3, 3)
        assertEquals("abXc", composition.text)
        assertEquals(3, composition.selectionStart)
    }

    @Test
    fun deletingTextKeepsTheRestOfTheStyles() {
        val bold = Composition(FormattedText("Hello world", listOf(StyleRange(6, 11, SpanStyle.Bold))))
        val result = bold.withText("Hello rld", 6, 6)
        assertEquals(listOf(StyleRange(6, 9, SpanStyle.Bold)), result.content.ranges)
    }

    @Test
    fun replacingASelectionWithTypedTextTakesTheStyleBeforeIt() {
        val start = Composition(FormattedText("ab cd", listOf(StyleRange(0, 2, SpanStyle.Bold))))
        val result = start.withText("abX", 3, 3)
        assertEquals("abX", result.text)
        assertEquals(listOf(StyleRange(0, 3, SpanStyle.Bold)), result.content.ranges)
    }

    @Test
    fun anEditResetsThePendingStyle() {
        val result = typed(Composition().toggle(SpanStyle.Bold), "a")
        assertNull(result.typing)
    }

    @Test
    fun insertingReplacesTheSelectionAndLeavesTheCursorAfterTheInsertion() {
        val composition = Composition(FormattedText("Hello world")).select(6, 11).insert("there")
        assertEquals("Hello there", composition.text)
        assertEquals(11, composition.selectionStart)
        assertEquals(11, composition.selectionEnd)
    }

    @Test
    fun insertingAtTheCursorKeepsTextOnBothSides() {
        val composition = Composition(FormattedText("Hello world")).select(5, 5).insert(",")
        assertEquals("Hello, world", composition.text)
        assertEquals(6, composition.selectionStart)
    }

    @Test
    fun insertingIntoBoldTextIsBold() {
        val composition = Composition(FormattedText("abcd", listOf(StyleRange(0, 4, SpanStyle.Bold)))).select(2, 2).insert("XX")
        assertEquals(listOf(StyleRange(0, 6, SpanStyle.Bold)), composition.content.ranges)
    }

    @Test
    fun surrogatePairsAreNeverSplitByAnEdit() {
        val start = Composition(FormattedText("a😀b"))
        val result = start.withText("a😁b", 3, 3)
        assertEquals("a😁b", result.text)
        assertEquals(4, result.text.length)
    }

    @Test
    fun anUnchangedTextOnlyMovesTheSelection() {
        val start = Composition(FormattedText("abc"))
        val result = start.withText("abc", 1, 2)
        assertSame(start.content, result.content)
        assertEquals(1, result.selectionStart)
        assertEquals(2, result.selectionEnd)
    }

    @Test
    fun blankCompositionIsBlank() {
        assertTrue(Composition().isBlank)
        assertTrue(Composition(FormattedText("  ")).isBlank)
        assertFalse(Composition(FormattedText("a")).isBlank)
    }
}
