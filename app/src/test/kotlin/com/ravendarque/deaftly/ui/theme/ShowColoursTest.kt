package com.ravendarque.deaftly.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.ravendarque.deaftly.core.contrast.WcagContrast
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val MESSAGE_CONTRAST = 7.0

class ShowColoursTest {
    @Test
    fun everySchemeHasAtLeastSevenToOneContrastForTheMessage() {
        ShowScheme.entries.forEach { scheme ->
            val ratio = WcagContrast.contrastRatio(scheme.colours.text.toArgb(), scheme.colours.background.toArgb())
            assertTrue(ratio >= MESSAGE_CONTRAST, "$scheme message contrast $ratio")
        }
    }

    @Test
    fun everySchemeHasAtLeastSevenToOneContrastForHighlightedText() {
        ShowScheme.entries.forEach { scheme ->
            val ratio = WcagContrast.contrastRatio(scheme.colours.markText.toArgb(), scheme.colours.markBackground.toArgb())
            assertTrue(ratio >= MESSAGE_CONTRAST, "$scheme highlight contrast $ratio")
        }
    }

    @Test
    fun everySchemeMeetsAaForTheSecondaryText() {
        ShowScheme.entries.forEach { scheme ->
            assertTrue(WcagContrast.meetsAa(scheme.colours.muted.toArgb(), scheme.colours.background.toArgb()), "$scheme muted text")
        }
    }

    @Test
    fun theDefaultFollowsTheTheme() {
        assertEquals(ShowScheme.BlackOnWhite, defaultShowScheme(darkTheme = false))
        assertEquals(ShowScheme.WhiteOnBlack, defaultShowScheme(darkTheme = true))
    }
}
