package com.ravendarque.deaftly.ui.home

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
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

    private fun capture(dark: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = HomeViewModel(FakeTemplateRepository(builtInTemplates(context.resources)))
        composeRule.setContent { AppTheme(darkTheme = dark) { TemplatesScreen(viewModel, onNavigate = {}, onHome = {}) } }
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
