package com.ravendarque.deaftly.ui.chat

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.templates.BuiltInIds
import com.ravendarque.deaftly.templates.builtInTemplates
import com.ravendarque.deaftly.ui.fill.FillScreen
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class TemplatePickerScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository
        get() = FakeTemplateRepository(builtInTemplates(ApplicationProvider.getApplicationContext<Context>().resources))

    private fun picker(dark: Boolean) {
        val viewModel = HomeViewModel(repository)
        composeRule.setContent {
            AppTheme(darkTheme = dark) { TemplatePickerScreen(viewModel, onFill = {}, onInsert = {}, onBack = {}) }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun pickerLightTheme() = picker(dark = false)

    @Test
    fun pickerDarkTheme() = picker(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun pickerLargeFontScale() = picker(dark = false)

    @Test
    fun fillForChatLightTheme() {
        val viewModel = FillViewModel(BuiltInIds.EXPLAIN_DEAF, repository, SavedStateHandle())
        composeRule.setContent {
            AppTheme { FillScreen(BuiltInIds.EXPLAIN_DEAF, viewModel, onNavigate = {}, onBack = {}, onInsert = {}) }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }
}
