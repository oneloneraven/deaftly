package com.ravendarque.deaftly.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.contrast.WcagContrast
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParticipantColoursTest {
    private fun contrast(
        foreground: Color,
        background: Color,
    ) = WcagContrast.contrastRatio(foreground.toArgb(), background.toArgb())

    @Test
    fun thePaletteOffersSixColours() {
        assertEquals(6, ColourRole.entries.size)
    }

    @Test
    fun everyColourIsDistinct() {
        assertEquals(
            ColourRole.entries.size,
            ColourRole.entries
                .map { it.colours(dark = false).container }
                .toSet()
                .size,
        )
    }

    @Test
    fun bubbleTextMeetsFourPointFiveToOneInEveryColour() {
        val failures =
            ColourRole.entries
                .flatMap { role ->
                    val colours = role.colours(dark = false)
                    listOf(
                        "$role text" to contrast(colours.onContainer, colours.container),
                        "$role quote" to contrast(colours.onContainerMuted, colours.container),
                        "$role send" to contrast(colours.onStrong, colours.strong),
                    )
                }.filter { (_, ratio) -> ratio < WcagContrast.AA_NORMAL_TEXT }
        assertTrue(failures.isEmpty(), "Below 4.5:1: $failures")
    }

    @Test
    fun theNameLabelMeetsFourPointFiveToOneOnTheGroundInBothThemes() {
        val failures =
            listOf(false to LightDeaftlyColors.ground, true to DarkDeaftlyColors.ground)
                .flatMap { (dark, ground) ->
                    ColourRole.entries.map { role -> "$role dark=$dark" to contrast(role.colours(dark).label, ground) }
                }.filter { (_, ratio) -> ratio < WcagContrast.AA_NORMAL_TEXT }
        assertTrue(failures.isEmpty(), "Below 4.5:1: $failures")
    }
}
