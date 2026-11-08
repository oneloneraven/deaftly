package com.ravendarque.deaftly.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.contrast.WcagContrast
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val MIN_LIGHTNESS_STEP = 1.3

class ParticipantColoursTest {
    private val themes = listOf("light" to (LightDeaftlyColors to false), "dark" to (DarkDeaftlyColors to true))

    private fun contrast(
        foreground: Color,
        background: Color,
    ) = WcagContrast.contrastRatio(foreground.toArgb(), background.toArgb())

    @Test
    fun thePaletteOffersFourToSixColours() {
        assertTrue(ColourRole.entries.size in 4..6)
    }

    @Test
    fun accentAndPersonBAreTheThemeTokens() {
        themes.forEach { (_, theme) ->
            val (colours, dark) = theme
            assertEquals(colours.personA, ColourRole.Accent.colours(colours, dark).container)
            assertEquals(colours.personB, ColourRole.PersonB.colours(colours, dark).container)
        }
    }

    @Test
    fun bubbleTextMeetsFourPointFiveToOneInEveryColourAndTheme() {
        val failures =
            themes
                .flatMap { (name, theme) ->
                    ColourRole.entries.flatMap { role ->
                        val colours = role.colours(theme.first, theme.second)
                        listOf(
                            "$name $role text" to contrast(colours.onContainer, colours.container),
                            "$name $role quote" to contrast(colours.onContainerMuted, colours.container),
                            "$name $role send" to contrast(colours.onStrong, colours.strong),
                        )
                    }
                }.filter { (_, ratio) -> ratio < WcagContrast.AA_NORMAL_TEXT }
        assertTrue(failures.isEmpty(), "Below 4.5:1: $failures")
    }

    @Test
    fun everyColourDiffersInLightnessFromTheOthersNotJustHue() {
        themes.forEach { (name, theme) ->
            val luminances =
                ColourRole.entries
                    .map { WcagContrast.relativeLuminance(it.colours(theme.first, theme.second).container.toArgb()) }
                    .sorted()
            val tightest = luminances.zipWithNext { low, high -> (high + 0.05) / (low + 0.05) }.min()
            assertTrue(tightest >= MIN_LIGHTNESS_STEP, "$name theme: neighbouring colours are only %.2f:1 apart".format(tightest))
        }
    }
}
