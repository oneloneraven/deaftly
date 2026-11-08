package com.ravendarque.deaftly.ui.editor

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class EditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository = EditorTestRepository(editorTemplates, originals = listOf(builtInOrderFood))
    private val exits = mutableListOf<EditorExit>()
    private lateinit var viewModel: EditorViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(id: String?) {
        viewModel = EditorViewModel(id, repository, SavedStateHandle(), editorStrings, newId = { "new-id" })
        composeRule.setContent { AppTheme { EditorScreen(viewModel) { exits += it } } }
        composeRule.waitForIdle()
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    private fun option(label: String) = composeRule.onNode(hasText(label) and isSelectable())

    private fun SemanticsNodeInteraction.replaceText(text: String) {
        performTextClearance()
        performTextInput(text)
    }

    private fun tap(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    @Test
    fun showsTheFieldsOfAnExistingTemplate() {
        show("my-note")

        composeRule.onNodeWithText("Edit template").assertExists()
        field("Name").assertExists()
        composeRule.onNodeWithText("My note").assertExists()
        option("Everyday").assertExists()
        option("Food & drink").assertExists()
        composeRule.onNodeWithText("Message").assertExists()
        composeRule.onNodeWithText("Placeholder").assertExists()
        composeRule.onNodeWithText("Save").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aNewTemplateHasNoSettingsCardOrTemplateActions() {
        show(null)

        composeRule.onNodeWithText("New template").assertExists()
        composeRule.onNodeWithText("Write what you want to say").assertExists()
        composeRule.onNodeWithText("Make a copy").assertDoesNotExist()
        composeRule.onNodeWithText("Delete template").assertDoesNotExist()
        composeRule.onNodeWithText("Restore the original template").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Delete placeholder").assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun pickingAnExistingCategoryChipSetsTheCategory() {
        show("my-note")

        option("Food & drink").performClick()

        assertEquals("Food & drink", viewModel.draft.category)
        option("Food & drink").assertIsSelected()
    }

    @Test
    fun aNewTemplateCanBeWrittenWithAPlaceholderAndSaved() {
        show(null)
        field("Name").performTextInput("Greeting")
        composeRule.onNode(hasSetTextAction() and hasText("Message")).performTextInput("Hello ")
        tap("Placeholder")

        composeRule.onNodeWithText("Placeholder: new placeholder").assertExists()
        field("Name in the message").replaceText("who")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<EditorExit>(EditorExit.Saved), exits)
        val saved = repository.snapshot.first { it.id == "new-id" }
        assertEquals("Greeting", saved.title)
        assertEquals("Hello {who}", saved.body)
        assertEquals(PlaceholderType.Typing, saved.placeholder("who").type)
    }

    @Test
    fun savingWithoutANameOrMessageShowsErrorsAndStays() {
        show(null)

        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Give the template a name").assertExists()
        composeRule.onNodeWithText("Write a message").assertExists()
        assertTrue(exits.isEmpty())
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun renamingAPlaceholderUpdatesTheMessage() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        field("Name in the message").replaceText("item")

        assertEquals("Hello {name}, I need {item}.", viewModel.draft.body)
        composeRule.onNodeWithText("Placeholder: item").assertExists()
    }

    @Test
    fun aClashingPlaceholderNameShowsAnError() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        field("Name in the message").replaceText("name")

        composeRule.onNodeWithText("Another placeholder already has that name").assertExists()
        assertEquals("Hello {name}, I need {thing}.", viewModel.draft.body)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theSettingsCardShowsTypeOptionsAndRepeatableForAChoicesPlaceholder() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        option("Choices").assertIsSelected()
        composeRule.onNodeWithText("Choice 1").assertExists()
        composeRule.onNodeWithText("Choice 2").assertExists()
        composeRule.onNodeWithText("water").assertExists()
        composeRule.onNodeWithText("Allow more than one").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun choicesCanBeAddedEditedAndRemoved() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        tap("Add a choice")
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction() and hasText("Choice 3")).performTextInput("a menu")
        composeRule.onNodeWithContentDescription("Remove choice 1").performScrollTo().performClick()

        assertEquals(listOf("a chair", "a menu"), viewModel.draft.selected?.options)
    }

    @Test
    fun switchingTheTypeHidesTheChoices() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        option("Camera").performScrollTo().performClick()

        composeRule.onNodeWithText("Choice 1").assertDoesNotExist()
        option("Camera").assertIsSelected()
        assertEquals(PlaceholderType.Camera, viewModel.draft.selected?.type)
    }

    @Test
    fun allowMoreThanOneTogglesRepeatable() {
        show("my-note")
        viewModel.edit { it.select("name") }
        composeRule.waitForIdle()

        tap("Allow more than one")

        assertTrue(viewModel.draft.selected?.repeatable == true)
        tap("Allow more than one")
        assertTrue(viewModel.draft.selected?.repeatable == false)
    }

    @Test
    fun deletingAPlaceholderRemovesItFromTheMessage() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Delete placeholder").performScrollTo().performClick()

        assertEquals("Hello {name}, I need .", viewModel.draft.body)
        composeRule.onNodeWithContentDescription("Delete placeholder").assertDoesNotExist()
    }

    @Test
    fun withTwoPlaceholdersAPickerSelectsWhichOneToEdit() {
        show("my-note")
        viewModel.edit { it.select("thing") }
        composeRule.waitForIdle()

        option("name").performScrollTo().performClick()

        assertEquals("name", viewModel.draft.selectedKey)
        composeRule.onNodeWithText("Placeholder: name").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun cancelWithoutChangesLeavesStraightAway() {
        show("my-note")

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<EditorExit>(EditorExit.Cancelled), exits)
    }

    @Test
    fun cancelWithChangesAsksBeforeDiscarding() {
        show("my-note")
        field("Name").performTextInput(" two")

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.onNodeWithText("Discard your changes?").assertExists()
        assertTrue(exits.isEmpty())
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.onNodeWithText("Discard your changes?").assertDoesNotExist()
        assertTrue(exits.isEmpty())

        composeRule.onNodeWithContentDescription("Cancel").performClick()
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<EditorExit>(EditorExit.Cancelled), exits)
        assertEquals("My note", repository.snapshot.first { it.id == "my-note" }.title)
    }

    @Test
    fun aBuiltInOffersRestoreAndCopyButNotDelete() {
        show("order-food")

        composeRule.onNodeWithText("Restore the original template").assertExists()
        composeRule.onNodeWithText("Make a copy").assertExists()
        composeRule.onNodeWithText("Delete template").assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun restoringAsksFirstThenPutsTheOriginalBack() {
        show("order-food")
        field("Name").replaceText("Changed")
        assertEquals("Changed", viewModel.draft.name)

        tap("Restore the original template")
        composeRule.onNodeWithText("Restore the original template?").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        assertEquals("Changed", viewModel.draft.name)

        composeRule.onNodeWithText("Restore").performClick()
        composeRule.waitForIdle()

        assertEquals("Order food", viewModel.draft.name)
        composeRule.onNodeWithText("Restore the original template?").assertDoesNotExist()
    }

    @Test
    fun deletingAsksFirstAndThenRemovesTheTemplate() {
        show("my-note")

        tap("Delete template")
        composeRule.onNodeWithText("Delete this template?").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        assertEquals(3, repository.snapshot.size)

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<EditorExit>(EditorExit.Deleted), exits)
        assertEquals(2, repository.snapshot.size)
    }

    @Test
    fun cancellingTheDeleteDialogKeepsTheTemplate() {
        show("my-note")

        tap("Delete template")
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete this template?").assertDoesNotExist()
        assertEquals(3, repository.snapshot.size)
        assertTrue(exits.isEmpty())
    }

    @Test
    fun makeACopyAddsItAndExitsWithItsId() {
        show("my-note")

        tap("Make a copy")
        composeRule.waitForIdle()

        assertEquals(listOf<EditorExit>(EditorExit.Copied("new-id")), exits)
        assertEquals("Copy of My note", repository.snapshot.first { it.id == "new-id" }.title)
    }

    @Test
    fun aMissingTemplateLeavesImmediately() {
        show("nope")

        assertEquals(listOf<EditorExit>(EditorExit.Cancelled), exits)
    }
}
