package com.ravendarque.deaftly.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Teal), ShowScheme.WhiteOnBlack, ChatLayout.Sides)
    private var backs = 0

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        val viewModel = SettingsViewModel(settings)
        composeRule.setContent { AppTheme { SettingsScreen(viewModel, onBack = { backs++ }) } }
        composeRule.waitForIdle()
    }

    private fun hasState(selected: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, selected)

    @Test
    fun showsTheStoredChoices() {
        composeRule.onNodeWithText("Side by side").assertIsSelected()
        composeRule.onNodeWithText("Split screen").assert(hasState(false))
        composeRule.onNodeWithText("White on black").assertIsSelected()
        composeRule
            .onNode(
                hasSetTextAction(),
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Alex")))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun choosingTheDefaultLayoutSavesIt() {
        composeRule.onNodeWithText("Split screen").performClick()
        composeRule.waitForIdle()

        assertEquals(ChatLayout.Split, settings.savedLayout)
        composeRule.onNodeWithText("Split screen").assertIsSelected()
    }

    @Test
    fun editingTheNameKeepsTheColour() {
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Sam")
        composeRule.waitForIdle()

        assertEquals(ChatProfile("Sam", ColourRole.Teal), settings.saved)
    }

    @Test
    fun pickingAColourKeepsTheName() {
        composeRule.onAllNodesWithContentDescription("Rose")[0].performClick()
        composeRule.waitForIdle()

        assertEquals(ChatProfile("Alex", ColourRole.Rose), settings.saved)
    }

    @Test
    fun choosingAShowSchemeSavesIt() {
        composeRule.onNodeWithText("Yellow on black").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Yellow on black").assertIsSelected()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun backLeavesTheScreen() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
