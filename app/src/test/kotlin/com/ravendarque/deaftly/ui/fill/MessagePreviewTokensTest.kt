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
        val tokens = previewTokens(listOf(FillPart.Text("My name is "), FillPart.Filled(name, "Sam"), FillPart.Text(" ok")))

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
                PreviewToken.Chip(name, "Your name", false, gapBefore = true, suffix = "."),
                PreviewToken.Word("Please", true),
            ),
            tokens,
        )
    }

    @Test
    fun adjacentChipsAreSeparatedByAGap() {
        val tokens = previewTokens(listOf(FillPart.Filled(name, "Sam"), FillPart.Empty(reply)))

        assertEquals(
            listOf(
                PreviewToken.Chip(name, "Sam", true, gapBefore = false),
                PreviewToken.Chip(reply, "How should they reply?", false, gapBefore = true),
            ),
            tokens,
        )
    }
}
