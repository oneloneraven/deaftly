package com.ravendarque.deaftly.ui.hub

import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class HubScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(dark: Boolean) {
        composeRule.setContent { AppTheme(darkTheme = dark) { HubScreen(onNavigate = {}) } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    private fun captureDialog(
        dark: Boolean,
        option: String = "Face to face",
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { HubScreen(onNavigate = {}) } }
        composeRule.onNodeWithContentDescription("About $option").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNode(isDialog()).captureRoboImage()
    }

    @Test
    fun dialogLightTheme() = captureDialog(dark = false)

    @Test
    fun dialogDarkTheme() = captureDialog(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun dialogLargeFontScale() = captureDialog(dark = false)

    @Test
    fun dialogHelpLightTheme() = captureDialog(dark = false, option = "Help")

    @Test
    @Config(fontScale = 2f)
    fun dialogLongestCopyLargeFontScale() = captureDialog(dark = false, option = "Use a template")

    @Test
    fun dialogShareLightTheme() = captureDialog(dark = false, option = "Share")

    @Test
    fun lightTheme() = capture(dark = false)

    @Test
    fun darkTheme() = capture(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun largeFontScale() = capture(dark = false)
}
