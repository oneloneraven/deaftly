package com.ravendarque.deaftly.ui.help

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.help.HelpRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LOAD_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class HelpScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository = HelpRepository(ApplicationProvider.getApplicationContext<Context>().assets)

    private fun capture(
        dark: Boolean,
        waitFor: String,
        screen: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { screen() } }
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) { composeRule.onAllNodesWithText(waitFor).fetchSemanticsNodes().isNotEmpty() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    private fun topics(dark: Boolean) = capture(dark, "Quick start") { HelpTopicsScreen(repository, onOpen = {}, onBack = {}) }

    private fun page(dark: Boolean) =
        capture(dark, "Using a template") { HelpPageScreen("templates", repository, onOpenTopic = {}, onBack = {}) }

    @Test
    fun topicsLightTheme() = topics(dark = false)

    @Test
    fun topicsDarkTheme() = topics(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun topicsLargeFontScale() = topics(dark = false)

    @Test
    fun pageLightTheme() = page(dark = false)

    @Test
    fun pageDarkTheme() = page(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun pageLargeFontScale() = page(dark = false)
}
