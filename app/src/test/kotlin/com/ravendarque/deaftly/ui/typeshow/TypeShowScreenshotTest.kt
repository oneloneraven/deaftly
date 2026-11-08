package com.ravendarque.deaftly.ui.typeshow

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.typeshow"
private const val MESSAGE = "Could I have a table for two by the window, please?\nWe are both vegetarian."

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class TypeShowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private fun shot(
        message: String,
        dark: Boolean,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) { TypeShowContent(TextFieldState(message), onShow = {}, onBack = {}) }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    @Test
    fun emptyLightTheme() = shot("", dark = false)

    @Test
    fun filledLightTheme() = shot(MESSAGE, dark = false)

    @Test
    fun filledDarkTheme() = shot(MESSAGE, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun filledLargeFontScale() = shot(MESSAGE, dark = false)
}
