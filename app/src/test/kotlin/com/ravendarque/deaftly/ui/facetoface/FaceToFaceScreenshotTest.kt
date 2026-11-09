package com.ravendarque.deaftly.ui.facetoface

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextRange
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
class FaceToFaceScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sent =
        listOf(
            "Hello. I’m deaf, so I’m typing to you.",
            "Could I have a table for two, please?",
            "By the window if you can.",
        )

    private fun capture(
        dark: Boolean,
        messages: List<String> = sent,
        typing: String = "And could we see the menu",
    ) {
        val field = TextFieldState(typing, TextRange(typing.length))
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                FaceToFaceContent(messages, field, onSend = {}, onEnd = {}, onContinue = {})
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

    @Test
    fun emptyLightTheme() = capture(dark = false, messages = emptyList(), typing = "")

    private val fourLines =
        "Could we have a table for two by the window, please? We are celebrating a birthday and would love " +
            "somewhere quiet, with a high chair if you have one, and the vegetarian menu as well."

    @Test
    fun fieldEmptyLightTheme() = capture(dark = false, typing = "")

    @Test
    fun fieldEmptyDarkTheme() = capture(dark = true, typing = "")

    @Test
    @Config(fontScale = 2f)
    fun fieldEmptyLargeFontScale() = capture(dark = false, typing = "")

    @Test
    fun fieldOneLineLightTheme() = capture(dark = false, typing = "Table for two")

    @Test
    fun fieldOneLineDarkTheme() = capture(dark = true, typing = "Table for two")

    @Test
    @Config(fontScale = 2f)
    fun fieldOneLineLargeFontScale() = capture(dark = false, typing = "Table for two")

    @Test
    fun fieldFourLinesLightTheme() = capture(dark = false, typing = fourLines)

    @Test
    fun fieldFourLinesDarkTheme() = capture(dark = true, typing = fourLines)

    @Test
    @Config(fontScale = 2f)
    fun fieldFourLinesLargeFontScale() = capture(dark = false, typing = fourLines)
}
