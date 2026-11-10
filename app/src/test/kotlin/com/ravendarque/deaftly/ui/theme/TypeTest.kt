package com.ravendarque.deaftly.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeTest {
    private fun fonts(family: FontFamily) = (family as FontListFontFamily).fonts.map { it as ResourceFont }

    @Test
    fun variationSettingsDoNotDependOnDensityOrFontScale() {
        val all = fonts(BricolageGrotesque) + fonts(AtkinsonHyperlegibleNext)
        assertTrue(all.isNotEmpty())
        all.forEach { font ->
            font.variationSettings.settings.forEach { setting ->
                assertEquals(
                    setting.axisName,
                    setting.toVariationValue(Density(1f, 1f)),
                    setting.toVariationValue(Density(3f, 2f)),
                    0f,
                )
            }
        }
    }
}
