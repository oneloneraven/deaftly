package com.ravendarque.deaftly.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.PersonalNames
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.core.settings.ThemeMode
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green), ShowScheme.WhiteOnBlack)
    private val builtIn = Template("a", "A", "Everyday", "Hello", builtIn = true)
    private val templates = FakeTemplateRepository(emptyList(), listOf(builtIn))
    private var backs = 0

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        val viewModel = SettingsViewModel(settings, templates)
        composeRule.setContent { AppTheme { SettingsScreen(viewModel, onBack = { backs++ }) } }
        composeRule.waitForIdle()
    }

    @Test
    fun showsTheStoredChoices() {
        composeRule.onNodeWithText("Split screen").assertDoesNotExist()
        composeRule.onNodeWithText("White on black").assertIsSelected()
        composeRule
            .onNode(hasSetTextAction() and hasText("Name (optional)", substring = true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Alex")))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun editingTheNameKeepsTheColour() {
        composeRule.onNode(hasSetTextAction() and hasText("Name (optional)", substring = true)).performTextReplacement("Sam")
        composeRule.waitForIdle()

        assertEquals(ChatProfile("Sam", ColourRole.Green), settings.saved)
    }

    @Test
    fun pickingAColourKeepsTheName() {
        composeRule.onAllNodesWithContentDescription("Red")[0].performClick()
        composeRule.waitForIdle()

        assertEquals(ChatProfile("Alex", ColourRole.Red), settings.saved)
    }

    @Test
    fun choosingAShowSchemeSavesIt() {
        composeRule.onNodeWithText("Yellow on black").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Yellow on black").assertIsSelected()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun restoreBuiltInTemplatesBringsBackHiddenOnesAndThenDisables() {
        composeRule
            .onNodeWithText("Restore built-in templates")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(builtIn), templates.snapshot)
        composeRule.onNodeWithText("Restore built-in templates").assertIsNotEnabled()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun appearanceOffersSystemLightAndDarkWithSystemSelectedByDefault() {
        listOf("System", "Light", "Dark").forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }

        composeRule.onNodeWithText("System").assertIsSelected()
        composeRule.onNodeWithText("Dark").assertIsNotSelected()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun choosingDarkSavesItAndSelectsIt() {
        composeRule.onNodeWithText("Dark").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(ThemeMode.Dark, runBlocking { settings.themeMode.first() })
        composeRule.onNodeWithText("Dark").assertIsSelected()
        composeRule.onNodeWithText("System").assertIsNotSelected()
    }

    @Test
    fun backLeavesTheScreen() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun theFirstNameAndSurnameSave() {
        composeRule.onNode(hasSetTextAction() and hasText("First name")).performTextReplacement("Amira")
        composeRule.onNode(hasSetTextAction() and hasText("Surname")).performTextReplacement("Hassan")
        composeRule.waitForIdle()

        assertEquals(PersonalNames("Amira", "Hassan"), settings.savedNames)
    }

    @Test
    fun theLegalNameFieldAppearsOnlyWhileTheBoxIsTicked() {
        composeRule.onNodeWithText("Full legal name").assertDoesNotExist()

        composeRule.onNodeWithText("My legal name is different").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(
                hasSetTextAction() and hasText("Full legal name"),
            ).performScrollTo()
            .performTextReplacement("Amira Fatima Hassan")
        composeRule.waitForIdle()

        assertEquals(PersonalNames(legalNameDiffers = true, legalName = "Amira Fatima Hassan"), settings.savedNames)

        composeRule.onNodeWithText("My legal name is different").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Full legal name").assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun usingTheFirstNameInChatsHidesTheChatNameFieldOnceThereIsAFirstName() {
        composeRule.onNode(hasSetTextAction() and hasText("First name")).performTextReplacement("Amira")
        composeRule.onNodeWithText("Use my first name in chats").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(PersonalNames(firstName = "Amira", useFirstNameInChats = true), settings.savedNames)
        composeRule.onNode(hasSetTextAction() and hasText("Name (optional)", substring = true)).assertDoesNotExist()

        composeRule.onNodeWithText("Use my first name in chats").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction() and hasText("Name (optional)", substring = true)).assertExists()
    }
}
