package com.ravendarque.deaftly.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class SettingsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(dark: Boolean) {
        val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green), ShowScheme.YellowOnBlack)
        val viewModel = SettingsViewModel(settings)
        composeRule.setContent { AppTheme(darkTheme = dark) { SettingsScreen(viewModel, onBack = {}) } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun lightTheme() = capture(dark = false)

    @Test
    fun darkTheme() = capture(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun largeFontScale() = capture(dark = false)
}
