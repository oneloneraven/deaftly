package com.ravendarque.deaftly.ui.fill

import kotlin.test.Test
import kotlin.test.assertEquals

class MessagePreviewTokensTest {
    private val name = checkIn.placeholder("name")
    private val reply = checkIn.placeholder("how to reply")

    @Test
    fun wordsCarryAGapOnlyWhenWhitespacePrecedesThem() {
        val tokens = previewTokens(listOf(FillPart.Text("Hi there")))

        assertEquals(listOf(PreviewToken.Word("Hi", false), PreviewToken.Word("there", true)), tokens)
    }

    @Test
    fun chipsAreSeparatedFromSurroundingWordsByTheWhitespaceInTheText() {
        val tokens = previewTokens(listOf(FillPart.Text("My name is "), FillPart.Filled(name, listOf("Sam")), FillPart.Text(" ok")))

        assertEquals(
            listOf(
                PreviewToken.Word("My", false),
                PreviewToken.Word("name", true),
                PreviewToken.Word("is", true),
                PreviewToken.Chip(name, "Sam", true, gapBefore = true),
                PreviewToken.Word("ok", true),
            ),
            tokens,
        )
    }

    @Test
    fun punctuationDirectlyAfterAChipStaysAttachedToIt() {
        val tokens = previewTokens(listOf(FillPart.Text("Name "), FillPart.Empty(name), FillPart.Text(". Please")))

        assertEquals(
            listOf(
                PreviewToken.Word("Name", false),
                PreviewToken.Chip(name, "name", false, gapBefore = true, suffix = "."),
                PreviewToken.Word("Please", true),
            ),
            tokens,
        )
    }

    @Test
    fun adjacentChipsAreSeparatedByAGap() {
        val tokens = previewTokens(listOf(FillPart.Filled(name, listOf("Sam")), FillPart.Empty(reply)))

        assertEquals(
            listOf(
                PreviewToken.Chip(name, "Sam", true, gapBefore = false),
                PreviewToken.Chip(reply, "how to reply", false, gapBefore = true),
            ),
            tokens,
        )
    }

    private val item = orderFood.placeholder("menu item")

    @Test
    fun repeatableValuesAreRemovableChipsJoinedLikeTheRenderer() {
        val tokens = previewTokens(listOf(FillPart.Text("Have "), FillPart.Filled(item, listOf("A", "B", "C")), FillPart.Text(" please?")))

        assertEquals(
            listOf(
                PreviewToken.Word("Have", false),
                PreviewToken.Chip(item, "A", true, gapBefore = true, suffix = ",", removeIndex = 0),
                PreviewToken.Chip(item, "B", true, gapBefore = true, removeIndex = 1),
                PreviewToken.Word("and", true),
                PreviewToken.Chip(item, "C", true, gapBefore = true, removeIndex = 2),
                PreviewToken.AddMore(item, gapBefore = true),
                PreviewToken.Word("please?", true),
            ),
            tokens,
        )
    }

    @Test
    fun twoRepeatableValuesAreJoinedWithAnd() {
        val tokens = previewTokens(listOf(FillPart.Filled(item, listOf("A", "B"))))

        assertEquals(
            listOf(
                PreviewToken.Chip(item, "A", true, gapBefore = false, removeIndex = 0),
                PreviewToken.Word("and", true),
                PreviewToken.Chip(item, "B", true, gapBefore = true, removeIndex = 1),
                PreviewToken.AddMore(item, gapBefore = true),
            ),
            tokens,
        )
    }
}
