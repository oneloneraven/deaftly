package com.ravendarque.deaftly.ui.website

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.template.PersonalField
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.fill.FillScreen
import com.ravendarque.deaftly.ui.fill.FillViewModel
import com.ravendarque.deaftly.ui.fill.checkIn
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import com.ravendarque.deaftly.ui.hub.HubScreen
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Test
import org.robolectric.annotation.Config

@Config(qualifiers = "en-rGB-w390dp-h844dp-xhdpi")
class MarketingScreenshotTest : WebsiteScreenshotTest() {
    @Test
    fun fillWithName() {
        val template =
            checkIn.copy(
                placeholders =
                    checkIn.placeholders.mapValues { (key, placeholder) ->
                        if (key == "name") placeholder.copy(fillsWith = PersonalField.FullName) else placeholder
                    },
            )
        val viewModel =
            FillViewModel(
                template.id,
                FakeTemplateRepository(listOf(template)),
                FakeSettingsRepository(names = EXAMPLE_NAMES),
                SavedStateHandle(),
            )
        composeRule.setContent {
            AppTheme(darkTheme = false) { FillScreen(template.id, viewModel, onNavigate = {}, onBack = {}) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("how to reply").performClick()
        composeRule.onNodeWithText("type on my phone").performClick()
        composeRule.waitForIdle()
        capture()
    }

    @Test
    fun addToHomeScreen() {
        composeRule.setContent { AppTheme(darkTheme = false) { HubScreen(onNavigate = {}, addToHomeScreen = {}) } }
        composeRule.onNodeWithContentDescription("About Listen for me").performScrollTo().performClick()
        composeRule.waitForIdle()
        capture()
    }
}
