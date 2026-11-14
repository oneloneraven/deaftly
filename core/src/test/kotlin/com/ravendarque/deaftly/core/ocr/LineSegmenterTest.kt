package com.ravendarque.deaftly.core.ocr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class LineSegmenterTest {
    private fun word(
        text: String,
        left: Float,
        right: Float,
        top: Float = 0f,
        bottom: Float = 20f,
    ) = BoundingBox(left, top, right, bottom).let { TextWord(text, it, corners = it.toQuad()) }

    private fun line(
        text: String,
        vararg words: TextWord,
    ): TextLine {
        val area =
            BoundingBox(words.minOf { it.boundingBox.left }, 0f, words.maxOf { it.boundingBox.right }, 20f)
        return TextLine(text, area, words.toList(), corners = area.toQuad(), angleDegrees = 0f, confidence = 0.9f)
    }

    @Test
    fun aLineWithOrdinarySpacingIsLeftWhole() {
        val original = line("Penne arrabbiata", word("Penne", 0f, 50f), word("arrabbiata", 58f, 150f))

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun leaderDotsSplitTheItemFromItsPriceAndAreDropped() {
        val original =
            line(
                "Penne arrabbiata ...... 11.00",
                word("Penne", 0f, 50f),
                word("arrabbiata", 58f, 150f),
                word("......", 158f, 200f),
                word("11.00", 208f, 250f),
            )

        val parts = LineSegmenter.split(original)

        assertEquals(listOf("Penne arrabbiata", "11.00"), parts.map { it.text })
        assertEquals(BoundingBox(0f, 0f, 150f, 20f), parts[0].boundingBox)
        assertEquals(BoundingBox(208f, 0f, 250f, 20f), parts[1].boundingBox)
        assertEquals(listOf("Penne", "arrabbiata"), parts[0].words.map { it.text })
        assertEquals(BoundingBox(0f, 0f, 150f, 20f).toQuad(), parts[0].corners)
        assertEquals(0.9f, parts[1].confidence)
    }

    @Test
    fun spacedLeaderDotsSplitTheLineAndAreDropped() {
        val original =
            line(
                "Penne . . . . 11.00",
                word("Penne", 0f, 50f),
                word(".", 56f, 60f),
                word(".", 66f, 70f),
                word(".", 76f, 80f),
                word(".", 86f, 90f),
                word("11.00", 96f, 140f),
            )

        assertEquals(listOf("Penne", "11.00"), LineSegmenter.split(original).map { it.text })
    }

    @Test
    fun ellipsisCharactersCountAsLeaders() {
        val original = line("Penne …… 11.00", word("Penne", 0f, 50f), word("……", 56f, 80f), word("11.00", 86f, 130f))

        assertEquals(listOf("Penne", "11.00"), LineSegmenter.split(original).map { it.text })
    }

    @Test
    fun aFewDotsAcrossAWideGapAreReadAsLeaders() {
        val original =
            line("Bruschetta . £6.50", word("Bruschetta", 0f, 100f), word(".", 108f, 112f), word("£6.50", 120f, 170f))

        assertEquals(listOf("Bruschetta", "£6.50"), LineSegmenter.split(original).map { it.text })
    }

    @Test
    fun aFewDotsAcrossANarrowGapAreKeptAsText() {
        val original = line("Est . 1987", word("Est", 0f, 30f), word(".", 32f, 36f), word("1987", 40f, 80f))

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun aMiddleDotSeparatorIsKeptAcrossAWideGap() {
        val original =
            line("Fresh bread · Cakes", word("Fresh", 0f, 50f), word("bread", 56f, 100f), word("·", 108f, 112f), word("Cakes", 120f, 170f))

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun anEllipsisOnAWordIsKept() {
        val original = line("Back soon... sorry", word("Back", 0f, 40f), word("soon...", 48f, 104f), word("sorry", 110f, 160f))

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun aWideGapBetweenColumnsSplitsTheLine() {
        val original = line("Chips £3.60", word("Chips", 0f, 50f), word("£3.60", 75f, 125f))

        assertEquals(listOf("Chips", "£3.60"), LineSegmenter.split(original).map { it.text })
    }

    @Test
    fun aGapNoWiderThanTheLimitKeepsTheLineWhole() {
        val limit = 20f * LineSegmenter.MAX_GAP_RATIO
        val original = line("Chips £3.60", word("Chips", 0f, 50f), word("£3.60", 50f + limit, 100f + limit))

        assertEquals(listOf("Chips £3.60"), LineSegmenter.split(original).map { it.text })
    }

    @Test
    fun theGapIsMeasuredAlongARotatedBaseline() {
        fun slanted(
            text: String,
            left: Float,
            right: Float,
        ): TextWord {
            val corners =
                Quad(listOf(Point(left, left), Point(right, right), Point(right - 20f, right + 20f), Point(left - 20f, left + 20f)))
            return TextWord(text, corners.boundingBox(), corners = corners)
        }
        val words = listOf(slanted("Tea", 0f, 30f), slanted("£2.20", 40f, 80f))
        val corners = Quad(listOf(Point(0f, 0f), Point(80f, 80f), Point(60f, 100f), Point(-20f, 20f)))
        val original = TextLine("Tea £2.20", corners.boundingBox(), words, corners = corners)

        assertEquals(listOf("Tea £2.20"), LineSegmenter.split(original).map { it.text })
    }

    @Test
    fun trailingLeadersAreTrimmedFromALoneItem() {
        val original = line("Starters ........", word("Starters", 0f, 80f), word("........", 88f, 200f))

        val part = LineSegmenter.split(original).single()

        assertEquals("Starters", part.text)
        assertEquals(BoundingBox(0f, 0f, 80f, 20f), part.boundingBox)
    }

    @Test
    fun aSingleSeparatorCharacterIsKeptAsText() {
        val original =
            line(
                "Freshly made · Fairly traded",
                word("Freshly", 0f, 60f),
                word("made", 68f, 110f),
                word("·", 118f, 124f),
                word("Fairly", 132f, 180f),
            )

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun aLineMadeOnlyOfLeadersIsLeftForTheTextFilter() {
        val original = line("........", word("........", 0f, 100f))

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun aLineWithoutWordsIsLeftWhole() {
        val original = TextLine("Soup 4.50", BoundingBox(0f, 0f, 100f, 20f))

        assertSame(original, LineSegmenter.split(original).single())
    }

    @Test
    fun aLineWithoutCornersGivesPartsWithoutCorners() {
        val original =
            TextLine(
                "Soup ...... 4.50",
                BoundingBox(0f, 0f, 200f, 20f),
                listOf(word("Soup", 0f, 40f), word("......", 48f, 150f), word("4.50", 158f, 200f)),
            )

        val parts = LineSegmenter.split(original)

        assertEquals(listOf("Soup", "4.50"), parts.map { it.text })
        parts.forEach { assertNull(it.corners) }
    }
}
