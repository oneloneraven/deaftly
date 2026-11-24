package com.oneloneraven.deaftly.ui.localisation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.oneloneraven.deaftly.core.chat.ChatProfile
import com.oneloneraven.deaftly.core.chat.ColourRole
import com.oneloneraven.deaftly.core.settings.ShowScheme
import com.oneloneraven.deaftly.templates.BuiltInIds
import com.oneloneraven.deaftly.templates.builtInTemplates
import com.oneloneraven.deaftly.ui.chat.FakeSettingsRepository
import com.oneloneraven.deaftly.ui.fill.FillScreen
import com.oneloneraven.deaftly.ui.fill.FillViewModel
import com.oneloneraven.deaftly.ui.home.FakeTemplateRepository
import com.oneloneraven.deaftly.ui.home.HomeViewModel
import com.oneloneraven.deaftly.ui.home.TemplatesScreen
import com.oneloneraven.deaftly.ui.hub.HubScreen
import com.oneloneraven.deaftly.ui.settings.SettingsScreen
import com.oneloneraven.deaftly.ui.settings.SettingsViewModel
import com.oneloneraven.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w390dp-h844dp")
class LocalisedScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val templates get() = builtInTemplates(RuntimeEnvironment.getApplication().resources)

    private fun capture(
        dark: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { content() } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    private fun hub(dark: Boolean) = capture(dark) { HubScreen(onNavigate = {}) }

    private fun templatesList(dark: Boolean) {
        val viewModel = HomeViewModel(FakeTemplateRepository(templates))
        capture(dark) { TemplatesScreen(viewModel, onNavigate = {}, onHome = {}) }
    }

    private fun interpreter(dark: Boolean) {
        val viewModel =
            FillViewModel(BuiltInIds.BSL_INTERPRETER, FakeTemplateRepository(templates), FakeSettingsRepository(), SavedStateHandle())
        capture(dark) { FillScreen(BuiltInIds.BSL_INTERPRETER, viewModel, onNavigate = {}, onBack = {}) }
    }

    private fun settings(dark: Boolean) {
        val repository = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green), ShowScheme.YellowOnBlack)
        val viewModel = SettingsViewModel(repository, FakeTemplateRepository(templates))
        capture(dark) { SettingsScreen(viewModel, onBack = {}) }
    }

    @Test
    fun hubLightTheme() = hub(dark = false)

    @Test
    fun hubDarkTheme() = hub(dark = true)

    @Test
    fun templatesLightTheme() = templatesList(dark = false)

    @Test
    fun templatesDarkTheme() = templatesList(dark = true)

    @Test
    fun interpreterTemplateLightTheme() = interpreter(dark = false)

    @Test
    fun interpreterTemplateDarkTheme() = interpreter(dark = true)

    @Test
    fun settingsLightTheme() = settings(dark = false)

    @Test
    fun settingsDarkTheme() = settings(dark = true)
}
