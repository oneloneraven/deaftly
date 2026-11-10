package com.ravendarque.deaftly.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val repository = EditorTestRepository(editorTemplates, originals = listOf(builtInOrderFood))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        id: String? = "my-note",
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = EditorViewModel(id, repository, savedState, editorStrings, newId = { "new-id" })

    private fun EditorViewModel.type(
        text: String,
        cursor: Int = text.length,
    ) = edit { it.withBody(TextFieldValue(text, TextRange(cursor))) }

    private fun stored(id: String) = repository.snapshot.first { it.id == id }

    @Test
    fun anExistingTemplateLoadsIntoACleanDraft() =
        runTest {
            val viewModel = viewModel()

            assertEquals(EditorPhase.Ready, viewModel.phase)
            assertEquals("My note", viewModel.draft.name)
            assertEquals("Hello {name}, I need {thing}.", viewModel.draft.body)
            assertEquals(listOf("Everyday", "Food & drink"), viewModel.categories)
            assertFalse(viewModel.isNew)
            assertFalse(viewModel.builtIn)
            assertFalse(viewModel.isDirty)
            assertNull(viewModel.draft.selectedKey)
        }

    @Test
    fun anUnknownTemplateIsMissing() =
        runTest {
            assertEquals(EditorPhase.Missing, viewModel("nope").phase)
        }

    @Test
    fun aNewTemplateStartsEmptyInTheFirstCategoryAndIsClean() =
        runTest {
            val viewModel = viewModel(null)

            assertTrue(viewModel.isNew)
            assertEquals("", viewModel.draft.name)
            assertEquals("Everyday", viewModel.draft.category)
            assertFalse(viewModel.isDirty)
        }

    @Test
    fun savingANewTemplateNeedsANameAndAMessage() =
        runTest {
            val viewModel = viewModel(null)

            viewModel.save()

            assertTrue(viewModel.showErrors)
            assertEquals(setOf(EditorError.NameBlank, EditorError.MessageBlank), viewModel.draft.errors)
            assertNull(viewModel.exit)
            assertEquals(editorTemplates, repository.snapshot)
        }

    @Test
    fun aBlankCategoryIsRejected() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.copy(category = "  ") }

            viewModel.save()

            assertEquals(setOf(EditorError.CategoryBlank), viewModel.draft.errors)
            assertNull(viewModel.exit)
        }

    @Test
    fun savingANewTemplateAddsItAndExits() =
        runTest {
            val viewModel = viewModel(null)
            viewModel.edit { it.copy(name = "  Greeting ", category = "Social") }
            viewModel.type("Hi")

            viewModel.save()

            assertEquals(EditorExit.Saved, viewModel.exit)
            val added = stored("new-id")
            assertEquals("Greeting", added.title)
            assertEquals("Social", added.category)
            assertEquals("Hi", added.body)
            assertFalse(added.pinned)
            assertFalse(added.builtIn)
        }

    @Test
    fun insertingAPlaceholderPutsATokenAtTheCursorAndOpensItsSettings() =
        runTest {
            val viewModel = viewModel(null)
            viewModel.type("Could I have  please?", cursor = 13)

            viewModel.edit { it.insertPlaceholder(editorStrings.newPlaceholderName) }

            assertEquals("Could I have {new placeholder} please?", viewModel.draft.body)
            assertEquals("new placeholder", viewModel.draft.selectedKey)
            assertEquals(DraftPlaceholder("new placeholder"), viewModel.draft.selected)
            assertEquals(30, viewModel.draft.selectionStart)
        }

    @Test
    fun aSecondInsertGetsAFreshName() =
        runTest {
            val viewModel = viewModel(null)

            viewModel.edit { it.insertPlaceholder(editorStrings.newPlaceholderName) }
            viewModel.edit { it.insertPlaceholder(editorStrings.newPlaceholderName) }

            assertEquals("{new placeholder}{new placeholder 2}", viewModel.draft.body)
            assertEquals("new placeholder 2", viewModel.draft.selectedKey)
        }

    @Test
    fun movingTheCursorIntoATokenSelectsIt() =
        runTest {
            val viewModel = viewModel()

            viewModel.type(viewModel.draft.body, cursor = 8)

            assertEquals("name", viewModel.draft.selectedKey)
            assertEquals("name", viewModel.draft.keyInput)
        }

    @Test
    fun typingATokenByHandCreatesATypingDefinition() =
        runTest {
            val viewModel = viewModel(null)

            viewModel.type("Hi {who}")

            assertEquals("who", viewModel.draft.selectedKey)
            assertEquals(DraftPlaceholder("who"), viewModel.draft.selected)
        }

    @Test
    fun renamingUpdatesTheTokenAndTheDefinition() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.select("thing") }

            viewModel.edit { it.renameSelected("item") }

            assertEquals("Hello {name}, I need {item}.", viewModel.draft.body)
            assertEquals("item", viewModel.draft.selectedKey)
            assertEquals(listOf("name", "item"), viewModel.draft.placeholders.map { it.key })
            assertEquals(listOf("water", "a chair"), viewModel.draft.selected?.options)
            assertNull(viewModel.draft.keyError)
        }

    @Test
    fun renamingChangesEveryTokenWithThatKeyAndKeepsTheCursorPlace() =
        runTest {
            val viewModel = viewModel(null)
            viewModel.type("{a} and {a} now")
            viewModel.type("{a} and {a} now", cursor = 14)
            viewModel.edit { it.select("a").renameSelected("long") }

            assertEquals("{long} and {long} now", viewModel.draft.body)
            assertEquals(viewModel.draft.body.length - 1, viewModel.draft.selectionStart)
        }

    @Test
    fun renamingToATakenNameIsRefusedWithAnError() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.select("thing").renameSelected("name") }

            assertEquals(KeyError.Taken, viewModel.draft.keyError)
            assertEquals("Hello {name}, I need {thing}.", viewModel.draft.body)
            assertEquals("thing", viewModel.draft.selectedKey)
            assertEquals("name", viewModel.draft.keyInput)
        }

    @Test
    fun renamingToABlankOrBracedNameIsRefused() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.select("thing").renameSelected("  ") }
            assertEquals(KeyError.Blank, viewModel.draft.keyError)

            viewModel.edit { it.renameSelected("a{b") }
            assertEquals(KeyError.Braces, viewModel.draft.keyError)
            assertEquals("Hello {name}, I need {thing}.", viewModel.draft.body)

            viewModel.edit { it.renameSelected("fixed") }
            assertNull(viewModel.draft.keyError)
            assertEquals("Hello {name}, I need {fixed}.", viewModel.draft.body)
        }

    @Test
    fun surroundingSpacesInANewNameAreTrimmed() =
        runTest {
            val viewModel = viewModel()

            viewModel.edit { it.select("thing").renameSelected(" item ") }

            assertEquals("Hello {name}, I need {item}.", viewModel.draft.body)
        }

    @Test
    fun deletingAPlaceholderRemovesItsTokensAndDefinition() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.select("thing") }

            viewModel.edit { it.deleteSelected() }

            assertEquals("Hello {name}, I need .", viewModel.draft.body)
            assertEquals(listOf("name"), viewModel.draft.placeholders.map { it.key })
            assertNull(viewModel.draft.selectedKey)
        }

    @Test
    fun backspacingIntoATokenRemovesTheWholeToken() =
        runTest {
            val viewModel = viewModel()

            viewModel.type("Hello {name}, I need {thing.", cursor = 27)

            assertEquals("Hello {name}, I need .", viewModel.draft.body)
            assertEquals(21, viewModel.draft.selectionStart)
            assertNull(viewModel.draft.selectedKey)
        }

    @Test
    fun backspacingATokenAlsoRemovesItsPlaceholder() =
        runTest {
            val viewModel = viewModel()

            viewModel.type("Hello {name}, I need {thing.", cursor = 27)

            assertEquals(listOf("name"), viewModel.draft.placeholders.map { it.key })
        }

    @Test
    fun deletingOneOfTwoTokensWithTheSameKeyKeepsThePlaceholder() =
        runTest {
            val viewModel = viewModel()
            viewModel.type("Hello {name}, {name}", cursor = 20)

            viewModel.type("Hello {name}, {name", cursor = 19)

            assertEquals(listOf("name"), viewModel.draft.placeholders.map { it.key })
        }

    @Test
    fun typingInsideATokenLandsAfterIt() =
        runTest {
            val viewModel = viewModel()

            viewModel.type("Hello {naxme}, I need {thing}.", cursor = 10)

            assertEquals("Hello {name}x, I need {thing}.", viewModel.draft.body)
        }

    @Test
    fun changingTypeOptionsAndRepeatableIsSavedWithTheTemplate() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.select("name") }
            viewModel.edit {
                it.updateSelected { p ->
                    p.copy(
                        type = PlaceholderType.Choices,
                        label = "  Who?  ",
                        options = listOf(" Sam ", "", "Alex", "Sam"),
                        repeatable = true,
                    )
                }
            }

            viewModel.save()

            assertEquals(
                Placeholder("name", "Who?", PlaceholderType.Choices, listOf("Sam", "Alex"), repeatable = true),
                stored("my-note").placeholder("name"),
            )
        }

    @Test
    fun optionsAreDroppedWhenThePlaceholderIsNotChoices() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.select("thing").updateSelected { p -> p.copy(type = PlaceholderType.Camera) } }

            viewModel.save()

            assertEquals(PlaceholderType.Camera, stored("my-note").placeholder("thing").type)
            assertEquals(emptyList(), stored("my-note").placeholder("thing").options)
        }

    @Test
    fun aBlankQuestionFallsBackToTheName() =
        runTest {
            val viewModel = viewModel(null)
            viewModel.edit { it.copy(name = "N", category = "C") }
            viewModel.edit { it.insertPlaceholder(editorStrings.newPlaceholderName) }

            viewModel.save()

            assertEquals("new placeholder", stored("new-id").placeholder("new placeholder").label)
        }

    @Test
    fun unknownTokensGetTypingDefinitionsAndUnusedDefinitionsAreDropped() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.copy(body = "Hi {name} and {stranger}") }

            viewModel.save()

            val saved = stored("my-note")
            assertEquals(setOf("name", "stranger"), saved.placeholders.keys)
            assertEquals(Placeholder("stranger", "stranger", PlaceholderType.Typing), saved.placeholders.getValue("stranger"))
        }

    @Test
    fun savingAnExistingTemplateUpdatesItAndKeepsItPinned() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.copy(name = "Renamed") }

            viewModel.save()

            assertEquals(EditorExit.Saved, viewModel.exit)
            assertEquals("Renamed", stored("my-note").title)
            assertTrue(stored("my-note").pinned)
            assertEquals(3, repository.snapshot.size)
        }

    @Test
    fun savingKeepsAPinChangedElsewhereWhileEditing() =
        runTest {
            val viewModel = viewModel()
            repository.setPinned("my-note", false)
            viewModel.edit { it.copy(name = "Renamed") }

            viewModel.save()

            assertFalse(stored("my-note").pinned)
        }

    @Test
    fun savingABuiltInKeepsItBuiltIn() =
        runTest {
            val viewModel = viewModel("order-food")
            viewModel.edit { it.copy(name = "Order a meal") }

            viewModel.save()

            assertTrue(stored("order-food").builtIn)
            assertEquals("Order a meal", stored("order-food").title)
        }

    @Test
    fun editsMakeTheDraftDirtyAndRevertingMakesItCleanAgain() =
        runTest {
            val viewModel = viewModel()

            viewModel.edit { it.copy(name = "Other") }
            assertTrue(viewModel.isDirty)

            viewModel.edit { it.copy(name = "My note") }
            assertFalse(viewModel.isDirty)
        }

    @Test
    fun discardingExitsWithoutTouchingTheRepository() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.copy(name = "Other") }

            viewModel.discard()

            assertEquals(EditorExit.Cancelled, viewModel.exit)
            assertEquals(editorTemplates, repository.snapshot)
        }

    @Test
    fun makingACopyAddsAnUnpinnedUserTemplateWithTheDraftContents() =
        runTest {
            val viewModel = viewModel("order-food")
            viewModel.edit { it.copy(name = "Order a meal") }

            viewModel.makeCopy()

            assertEquals(EditorExit.Copied("new-id"), viewModel.exit)
            val copy = stored("new-id")
            assertEquals("Copy of Order a meal", copy.title)
            assertFalse(copy.builtIn)
            assertFalse(copy.pinned)
            assertEquals(builtInOrderFood.body, copy.body)
            assertEquals(builtInOrderFood.placeholders, copy.placeholders)
            assertEquals("Order food", stored("order-food").title)
        }

    @Test
    fun makingACopyOfAnInvalidDraftShowsErrors() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.copy(name = "") }

            viewModel.makeCopy()

            assertTrue(viewModel.showErrors)
            assertNull(viewModel.exit)
            assertEquals(3, repository.snapshot.size)
        }

    @Test
    fun deletingAUserTemplateRemovesItAndExits() =
        runTest {
            val viewModel = viewModel()

            viewModel.delete()

            assertEquals(EditorExit.Deleted, viewModel.exit)
            assertEquals(listOf("check-in", "order-food"), repository.snapshot.map { it.id })
        }

    @Test
    fun aBuiltInCanBeDeletedWhichHidesIt() =
        runTest {
            val viewModel = viewModel("order-food")

            viewModel.delete()

            assertEquals(EditorExit.Deleted, viewModel.exit)
            assertEquals(listOf("check-in", "my-note"), repository.snapshot.map { it.id })
        }

    @Test
    fun restoringABuiltInPutsTheOriginalBackAndCleansTheDraft() =
        runTest {
            val viewModel = viewModel("order-food")
            viewModel.edit { it.copy(name = "Changed", body = "Something {else}") }
            viewModel.save()

            val again = viewModel("order-food")
            again.restoreOriginal()

            assertEquals(builtInOrderFood, stored("order-food"))
            assertEquals("Order food", again.draft.name)
            assertFalse(again.isDirty)
            assertNull(again.exit)
        }

    @Test
    fun restoreDoesNothingForATemplateTheUserMade() =
        runTest {
            val viewModel = viewModel()
            viewModel.edit { it.copy(name = "Other") }

            viewModel.restoreOriginal()

            assertEquals("Other", viewModel.draft.name)
        }

    @Test
    fun theDraftSurvivesRecreationThroughTheSavedStateHandle() =
        runTest {
            val handle = SavedStateHandle()
            val first = viewModel(savedState = handle)
            first.edit { it.copy(name = "In progress") }
            first.type("Hello {name}, I need {thing} too.", cursor = 8)

            val second = viewModel(savedState = handle)

            assertEquals(first.draft, second.draft)
            assertEquals("In progress", second.draft.name)
            assertTrue(second.isDirty)
        }

    @Test
    fun aCorruptSavedDraftIsIgnored() =
        runTest {
            val handle = SavedStateHandle(mapOf("draft" to "not json"))

            val viewModel = viewModel(savedState = handle)

            assertEquals("My note", viewModel.draft.name)
        }
}
