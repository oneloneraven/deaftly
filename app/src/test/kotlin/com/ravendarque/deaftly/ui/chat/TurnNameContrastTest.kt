package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import com.ravendarque.deaftly.core.contrast.WcagContrast
import com.ravendarque.deaftly.ui.theme.DarkDeaftlyColors
import com.ravendarque.deaftly.ui.theme.LightDeaftlyColors
import org.junit.Test
import kotlin.test.assertTrue

class TurnNameContrastTest {
    @Test
    fun theDimmedNameStillMeetsFourPointFiveToOneOnTheGroundInBothThemes() {
        val ratios =
            listOf("light" to LightDeaftlyColors, "dark" to DarkDeaftlyColors).associate { (theme, colours) ->
                val dimmed = colours.ink.copy(alpha = INACTIVE_NAME_ALPHA).compositeOver(colours.ground)
                theme to WcagContrast.contrastRatio(dimmed.toArgb(), colours.ground.toArgb())
            }

        assertTrue(ratios.values.all { it >= WcagContrast.AA_NORMAL_TEXT }, "Below 4.5:1: $ratios")
    }

    @Test
    fun theDimmedNameIsStillVisiblyLighterThanTheActiveOne() {
        val dimmed = LightDeaftlyColors.ink.copy(alpha = INACTIVE_NAME_ALPHA).compositeOver(LightDeaftlyColors.ground)

        assertTrue(WcagContrast.contrastRatio(dimmed.toArgb(), LightDeaftlyColors.ink.toArgb()) > 2.0)
    }
}
