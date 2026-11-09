package com.ravendarque.deaftly.ui.firstrun

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class FirstRunScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(dark: Boolean) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                FirstRunContent(
                    state = FirstRunState("Alex", ColourRole.Green, ChatLayout.Sides),
                    onName = {},
                    onColour = {},
                    onLayout = {},
                    onGetStarted = {},
                )
            }
        }
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
