package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RichTextTest {
    private val bold = setOf(SpanStyle.Bold)

    @Test
    fun plainTextIsOneUnstyledSpan() {
        assertEquals(listOf(Span("Hello there")), RichText.spans("Hello there"))
    }

    @Test
    fun emptyTextHasNoSpans() {
        assertTrue(RichText.spans("").isEmpty())
    }

    @Test
    fun styleRangeSplitsTextIntoRuns() {
        val spans = RichText.spans("Hello brave world", listOf(StyleRange(6, 11, SpanStyle.Bold)))
        assertEquals(listOf(Span("Hello "), Span("brave", bold), Span(" world")), spans)
    }

    @Test
    fun rangeAtStartAndEndLeavesNoEmptyRuns() {
        val spans = RichText.spans("abc", listOf(StyleRange(0, 3, SpanStyle.Italic)))
        assertEquals(listOf(Span("abc", setOf(SpanStyle.Italic))), spans)
    }

    @Test
    fun emptyRangeIsIgnored() {
        assertEquals(listOf(Span("abc")), RichText.spans("abc", listOf(StyleRange(1, 1, SpanStyle.Bold))))
    }

    @Test
    fun overlappingRangesCombineStyles() {
        val spans =
            RichText.spans(
                "abcdef",
                listOf(StyleRange(0, 4, SpanStyle.Bold), StyleRange(2, 6, SpanStyle.Highlight)),
            )
        assertEquals(
            listOf(
                Span("ab", bold),
                Span("cd", setOf(SpanStyle.Bold, SpanStyle.Highlight)),
                Span("ef", setOf(SpanStyle.Highlight)),
            ),
            spans,
        )
    }

    @Test
    fun adjacentRangesWithTheSameStyleMerge() {
        val spans =
            RichText.spans(
                "abcd",
                listOf(StyleRange(0, 2, SpanStyle.Bold), StyleRange(2, 4, SpanStyle.Bold)),
            )
        assertEquals(listOf(Span("abcd", bold)), spans)
    }

    @Test
    fun duplicateRangesDoNotDuplicateStyles() {
        val spans =
            RichText.spans(
                "ab",
                listOf(StyleRange(0, 2, SpanStyle.Bold), StyleRange(0, 2, SpanStyle.Bold)),
            )
        assertEquals(listOf(Span("ab", bold)), spans)
    }

    @Test
    fun rangeOrderDoesNotMatter() {
        val ranges = listOf(StyleRange(2, 4, SpanStyle.Italic), StyleRange(0, 3, SpanStyle.Bold))
        assertEquals(RichText.spans("abcde", ranges), RichText.spans("abcde", ranges.reversed()))
    }

    @Test
    fun rangeBeyondTheTextIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            RichText.spans("abc", listOf(StyleRange(1, 4, SpanStyle.Bold)))
        }
    }

    @Test
    fun invertedOrNegativeRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> { StyleRange(3, 2, SpanStyle.Bold) }
        assertFailsWith<IllegalArgumentException> { StyleRange(-1, 2, SpanStyle.Bold) }
    }

    @Test
    fun rangeSplittingASurrogatePairIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            RichText.spans("a😀b", listOf(StyleRange(0, 2, SpanStyle.Bold)))
        }
    }

    @Test
    fun rangeAroundASurrogatePairIsAccepted() {
        val spans = RichText.spans("a😀b", listOf(StyleRange(1, 3, SpanStyle.Bold)))
        assertEquals(listOf(Span("a"), Span("😀", bold), Span("b")), spans)
    }

    @Test
    fun emptySpanIsRejected() {
        assertFailsWith<IllegalArgumentException> { Span("") }
    }

    @Test
    fun plainTextJoinsSpansWithoutStyling() {
        val spans = RichText.spans("Hello brave world", listOf(StyleRange(6, 11, SpanStyle.Bold)))
        assertEquals("Hello brave world", RichText.plainText(spans))
    }

    @Test
    fun plainTextOfNoSpansIsEmpty() {
        assertEquals("", RichText.plainText(emptyList()))
    }
}
