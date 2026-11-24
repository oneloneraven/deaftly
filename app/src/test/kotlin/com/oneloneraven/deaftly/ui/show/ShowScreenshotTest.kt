package com.oneloneraven.deaftly.ui.show

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.oneloneraven.deaftly.core.settings.ShowScheme
import com.oneloneraven.deaftly.core.settings.ShowTextSize
import com.oneloneraven.deaftly.navigation.HighlightRange
import com.oneloneraven.deaftly.ui.theme.AppTheme
import com.oneloneraven.deaftly.ui.theme.defaultShowScheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.oneloneraven.deaftly.ui.show"
private const val MESSAGE = "Hi! I'm deaf. I'd like to check in. My name is Sam.\n\nPlease type on my phone."
private const val LONG_MESSAGE =
    "Hi! I'm deaf. I'd like to check in. My name is Sam. I have a booking for two people at half past seven, " +
        "under the name Sam. We would like a table by the window if one is free, and we are both vegetarian. " +
        "Could you please type any questions on my phone, or write them down, because I can't hear you and I can't " +
        "read lips well. Thank you very much for your patience, it really helps."

private const val LONG_WORDS =
    "Hi! I'm deaf. I have an appointment at 10:30 with Dr Shah. I prefer to communicate by typing."

private val MESSAGE_HIGHLIGHTS =
    listOf("Sam", "type on my phone").map { HighlightRange(MESSAGE.indexOf(it), MESSAGE.indexOf(it) + it.length) }

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class ShowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private fun shot(
        text: String,
        dark: Boolean,
        scheme: ShowScheme = defaultShowScheme(dark),
        highlights: List<HighlightRange> = MESSAGE_HIGHLIGHTS,
        textSize: ShowTextSize = ShowTextSize.Large,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                ShowContent(
                    text,
                    scheme,
                    onCycleScheme = {},
                    onClose = {},
                    onContinueAsChat = {},
                    highlights = highlights,
                    textSize = textSize,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    @Test
    fun shortTextLightTheme() = shot(MESSAGE, dark = false)

    @Test
    fun shortTextDarkTheme() = shot(MESSAGE, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun shortTextLargeFontScale() = shot(MESSAGE, dark = false)

    @Test
    fun withoutHighlights() = shot(MESSAGE, dark = false, highlights = emptyList())

    @Test
    @Config(fontScale = 2f)
    fun yellowOnBlackLargeFontScale() = shot(MESSAGE, dark = false, scheme = ShowScheme.YellowOnBlack)

    @Test
    @Config(fontScale = 2f)
    fun whiteOnBlackLargeFontScale() = shot(MESSAGE, dark = false, scheme = ShowScheme.WhiteOnBlack)

    @Test
    fun longTextLightTheme() = shot(LONG_MESSAGE, dark = false)

    @Test
    fun longTextDarkTheme() = shot(LONG_MESSAGE, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun longTextLargeFontScale() = shot(LONG_MESSAGE, dark = false)

    @Test
    fun blackOnWhiteScheme() = shot(MESSAGE, dark = true, scheme = ShowScheme.BlackOnWhite)

    @Test
    fun whiteOnBlackScheme() = shot(MESSAGE, dark = false, scheme = ShowScheme.WhiteOnBlack)

    @Test
    fun yellowOnBlackScheme() = shot(MESSAGE, dark = false, scheme = ShowScheme.YellowOnBlack)

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h800dp")
    fun longWordsNarrowScreen() = shot(LONG_WORDS, dark = false, highlights = emptyList())

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h800dp", fontScale = 2f)
    fun longWordsNarrowScreenLargeFontScale() = shot(LONG_WORDS, dark = false, highlights = emptyList())

    @Test
    fun mediumTextLightTheme() = shot(MESSAGE, dark = false, textSize = ShowTextSize.Medium)

    @Test
    fun smallTextLightTheme() = shot(MESSAGE, dark = false, textSize = ShowTextSize.Small)

    @Test
    @Config(fontScale = 2f)
    fun smallTextLargeFontScale() = shot(MESSAGE, dark = false, textSize = ShowTextSize.Small)
}
