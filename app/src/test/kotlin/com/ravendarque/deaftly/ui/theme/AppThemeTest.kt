package com.ravendarque.deaftly.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.settings.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AppThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun applied(mode: ThemeMode): Pair<Boolean, Color> {
        var result: Pair<Boolean, Color>? = null
        composeRule.setContent {
            AppTheme(mode) { result = LocalDarkTheme.current to Deaftly.colors.ground }
        }
        composeRule.waitForIdle()
        return checkNotNull(result)
    }

    @Test
    fun darkAppliesTheDarkPaletteOnALightPhone() {
        assertEquals(true to DarkDeaftlyColors.ground, applied(ThemeMode.Dark))
    }

    @Test
    @Config(qualifiers = "night")
    fun lightAppliesTheLightPaletteOnADarkPhone() {
        assertEquals(false to LightDeaftlyColors.ground, applied(ThemeMode.Light))
    }

    @Test
    fun systemFollowsALightPhone() {
        assertEquals(false to LightDeaftlyColors.ground, applied(ThemeMode.System))
    }

    @Test
    @Config(qualifiers = "night")
    fun systemFollowsADarkPhone() {
        assertEquals(true to DarkDeaftlyColors.ground, applied(ThemeMode.System))
    }
}
