package com.ravendarque.deaftly.ui.fill

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
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
class FillScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigated = mutableListOf<Route>()
    private var backs = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(templateId: String) {
        val viewModel = FillViewModel(templateId, FakeTemplateRepository(fillTemplates), SavedStateHandle())
        composeRule.setContent {
            AppTheme { FillScreen(templateId, viewModel, onNavigate = { navigated += it }, onBack = { backs++ }) }
        }
        composeRule.waitForIdle()
    }

    private fun chip(label: String) = composeRule.onNode(hasText(label) and hasClickAction())

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun SemanticsNodeInteraction.assertState(text: String) = assert(hasState(text))

    private fun option(label: String) = composeRule.onNode(hasText(label) and isSelectable())

    private fun fillNameThenReply() {
        chip("name").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Sam")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        option("write it down").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun showsTheTitleTheMessageTheHintAndEmptyChips() {
        show("check-in")

        composeRule.onNodeWithText("Check in").assertExists()
        composeRule
            .onNodeWithText(
                "Hi! I'm deaf. I'd like to check in. My name is Your name. Please How should they reply?.",
            ).assertExists()
        composeRule.onNodeWithText("Tap a highlighted word to change it.").assertExists()
        chip("name").assertState("Empty")
        chip("how to reply").assertState("Empty")
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun showIsDisabledUntilEveryPlaceholderIsFilled() {
        show("check-in")
        composeRule.onNodeWithText("Show").assertIsNotEnabled()

        chip("name").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Sam")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Show").assertIsNotEnabled()

        option("write it down").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Show").assertIsEnabled()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun tappingAChipOpensItsSheetWithTheOptionsBeforeSomethingElse() {
        show("check-in")

        chip("how to reply").performClick()

        chip("how to reply").assertState("Editing")
        composeRule.onNodeWithText("How should they reply?").assertExists()
        replyOptions.forEach { option(it).assertExists() }
        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun choosingAnOptionFillsTheChipAndShowsItSelectedWhenReopened() {
        show("check-in")
        chip("how to reply").performClick()

        option("write it down").performClick()
        composeRule.waitForIdle()

        chip("write it down").assertState("Filled in")
        chip("name").assertState("Editing")
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun aCustomValueFromSomethingElseFillsTheChip() {
        show("check-in")
        chip("how to reply").performClick()

        composeRule.onNode(hasSetTextAction()).performTextInput("shout")
        composeRule.onNodeWithContentDescription("Use this").performClick()
        composeRule.waitForIdle()

        chip("shout").assertState("Filled in")
    }

    @Test
    fun theCurrentValueIsMarkedSelectedInTheSheet() {
        show("check-in")
        chip("how to reply").performClick()
        option("write it down").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).performTextInput("Sam")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()

        chip("write it down").performClick()

        option("write it down").assertIsSelected()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aTypingPlaceholderOpensWithItsFieldFocused() {
        show("check-in")

        chip("name").performClick()

        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun fillingEverythingClosesTheSheetAndShowPassesTheText() {
        show("check-in")
        fillNameThenReply()

        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
        composeRule.onNodeWithText("Hi! I'm deaf. I'd like to check in. My name is Sam. Please write it down.").assertExists()
        composeRule.onNodeWithText("Show").performClick()

        assertEquals(listOf<Route>(Route.Show("Hi! I'm deaf. I'd like to check in. My name is Sam. Please write it down.")), navigated)
    }

    @Test
    fun editAndBackNavigate() {
        show("check-in")

        composeRule.onNodeWithContentDescription("Edit template").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf<Route>(Route.Editor("check-in")), navigated)
        assertEquals(1, backs)
    }

    @Test
    fun aCameraPlaceholderOffersScanAndTypeIt() {
        show("order-food")

        chip("menu item").performClick()

        composeRule.onNodeWithText("Scan the menu").assertExists()
        composeRule.onNodeWithText("Type it").assertExists()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun scanTheMenuOpensTheOcrScreen() {
        show("order-food")
        chip("menu item").performClick()

        composeRule.onNodeWithText("Scan the menu").performClick()

        assertEquals(listOf<Route>(Route.Ocr), navigated)
    }

    @Test
    fun typeItRevealsTheFieldAndFillsTheChip() {
        show("order-food")
        chip("menu item").performClick()

        composeRule.onNodeWithText("Type it").performClick()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        composeRule.onNode(hasSetTextAction()).performTextInput("Pasta arrabbiata")
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()

        chip("Pasta arrabbiata").assertState("Filled in")
        composeRule.onNodeWithText("Show").assertIsEnabled()
    }

    private fun addMenuItem(
        opener: () -> Unit,
        name: String,
    ) {
        opener()
        composeRule.onNodeWithText("Type it").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput(name)
        composeRule.onNode(hasSetTextAction()).performImeAction()
        composeRule.waitForIdle()
    }

    private fun firstItem() = addMenuItem({ chip("menu item").performClick() }, "Pasta arrabbiata")

    private fun anotherItem(name: String) = addMenuItem({ composeRule.onNodeWithText("add more").performClick() }, name)

    private val joined =
        "Hi! I'm deaf. Could I have Pasta arrabbiata and Garlic bread please? " +
            "If you have a question, please type it on my phone."

    @Test
    fun aRepeatableValueIsARemovableChipFollowedByAddMore() {
        show("order-food")

        firstItem()

        chip("Pasta arrabbiata").assertState("Filled in")
        composeRule.onNodeWithContentDescription("Remove Pasta arrabbiata").assertExists()
        composeRule.onNodeWithText("add more").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun addMoreAddsAValueAndThePreviewJoinsThemInPlainEnglish() {
        show("order-food")
        firstItem()

        anotherItem("Garlic bread")

        chip("Garlic bread").assertState("Filled in")
        composeRule.onNodeWithText(joined).assertExists()
        composeRule.onNodeWithText("Show").assertIsEnabled()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun removingAValueUpdatesThePreviewAndRemovingTheLastOneDisablesShow() {
        show("order-food")
        firstItem()
        anotherItem("Garlic bread")

        composeRule.onNodeWithContentDescription("Remove Pasta arrabbiata").performClick()
        composeRule.onNodeWithText(joined.replace("Pasta arrabbiata and ", "")).assertExists()
        composeRule.onNodeWithText("Pasta arrabbiata").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Remove Garlic bread").performClick()
        chip("menu item").assertState("Empty")
        composeRule.onNodeWithText("add more").assertDoesNotExist()
        composeRule.onNodeWithText("Show").assertIsNotEnabled()
    }

    @Test
    fun showPassesTheJoinedText() {
        show("order-food")
        firstItem()
        anotherItem("Garlic bread")

        composeRule.onNodeWithText("Show").performClick()

        assertEquals(listOf<Route>(Route.Show(joined)), navigated)
    }

    @Test
    fun whenInsertingIntoAChatTheMainButtonInsertsTheFilledMessage() {
        val inserted = mutableListOf<String>()
        val viewModel = FillViewModel("check-in", FakeTemplateRepository(fillTemplates), SavedStateHandle())
        composeRule.setContent {
            AppTheme {
                FillScreen("check-in", viewModel, onNavigate = { navigated += it }, onBack = { backs++ }, onInsert = { inserted += it })
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Show").assertDoesNotExist()
        composeRule.onNodeWithText("Insert into chat").assertIsNotEnabled()

        fillNameThenReply()
        composeRule.onNodeWithText("Insert into chat").assertIsEnabled().performClick()

        assertEquals(listOf("Hi! I'm deaf. I'd like to check in. My name is Sam. Please write it down."), inserted)
        assertEquals(emptyList(), navigated)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
