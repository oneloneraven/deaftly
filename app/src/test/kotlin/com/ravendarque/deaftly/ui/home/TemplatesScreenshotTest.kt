package com.ravendarque.deaftly.ui.home

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.templates.builtInTemplates
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class TemplatesScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        dark: Boolean,
        empty: Boolean = false,
        scrolledToEnd: Boolean = false,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val templates = if (empty) emptyList() else builtInTemplates(context.resources)
        val viewModel = HomeViewModel(FakeTemplateRepository(templates))
        composeRule.setContent { AppTheme(darkTheme = dark) { TemplatesScreen(viewModel, onNavigate = {}, onHome = {}) } }
        composeRule.waitForIdle()
        if (scrolledToEnd) composeRule.onNodeWithText("Add new template").performScrollTo()
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
    fun endOfListLightTheme() = capture(dark = false, scrolledToEnd = true)

    @Test
    fun endOfListDarkTheme() = capture(dark = true, scrolledToEnd = true)

    @Test
    @Config(fontScale = 2f)
    fun endOfListLargeFontScale() = capture(dark = false, scrolledToEnd = true)

    @Test
    fun emptyListLightTheme() = capture(dark = false, empty = true)

    @Test
    fun emptyListDarkTheme() = capture(dark = true, empty = true)

    @Test
    @Config(fontScale = 2f)
    fun emptyListLargeFontScale() = capture(dark = false, empty = true)
}
