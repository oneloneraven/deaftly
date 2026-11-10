package com.ravendarque.deaftly.ui.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class TemplatesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigated = mutableListOf<Route>()
    private var homes = 0
    private val repository = FakeTemplateRepository(homeTemplates)

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        val viewModel = HomeViewModel(repository)
        composeRule.setContent { AppTheme { TemplatesScreen(viewModel, onNavigate = { navigated += it }, onHome = { homes++ }) } }
        composeRule.waitForIdle()
    }

    private fun count(text: String) = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun showsPinnedTilesCategoriesAndTheTemplateList() {
        composeRule.onNodeWithText("PINNED").assertExists()
        listOf("All", "Everyday", "Food & drink", "Health").forEach { composeRule.onNodeWithText(it).assertExists() }
        assertEquals(2, count("Check in"))
        assertEquals(1, count("Explain I'm deaf"))
        composeRule.onNodeWithText("All").assertIsSelected()
        composeRule.onNodeWithText("Everyday").assertIsNotSelected()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun searchFiltersTheListByTitleAndText() {
        composeRule.onNode(hasSetTextAction()).performTextInput("doctor")

        composeRule.onNodeWithText("At the doctor's").assertExists()
        assertEquals(0, count("Explain I'm deaf"))
        assertEquals(0, count("Check in"))

        composeRule.onNode(hasSetTextAction()).performTextInput("zz")
        composeRule.onNodeWithText("No templates match “doctorzz”").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun clearSearchRestoresTheList() {
        composeRule.onNode(hasSetTextAction()).performTextInput("nothing like this")
        composeRule.onNodeWithText("Clear search").performClick()

        composeRule.onNodeWithText("Clear search").assertDoesNotExist()
        assertEquals(1, count("Explain I'm deaf"))
        assertEquals(2, count("Check in"))
    }

    @Test
    fun categoryChipFiltersTheListAndIsSelected() {
        composeRule.onNodeWithText("Health").performClick()

        composeRule.onNodeWithText("Health").assertIsSelected()
        composeRule.onNodeWithText("All").assertIsNotSelected()
        composeRule.onNodeWithText("At the doctor's").assertExists()
        assertEquals(0, count("Explain I'm deaf"))
        assertEquals(1, count("Check in"))

        composeRule.onNodeWithText("All").performClick()
        assertEquals(1, count("Explain I'm deaf"))
    }

    @Test
    fun unfilledPlaceholdersShowTheirLabelsInTheCard() {
        composeRule.onNodeWithText("how to reply", substring = true).assertExists()
    }

    @Test
    fun tappingAListCardOpensFillWithItsId() {
        composeRule.onNodeWithText("At the doctor's").performClick()

        assertEquals(listOf<Route>(Route.Fill("doctor")), navigated)
    }

    @Test
    fun tappingAPinnedTileOpensFillWithItsId() {
        composeRule.onAllNodesWithText("Order food", substring = true)[0].performClick()

        assertEquals(listOf<Route>(Route.Fill("order-food")), navigated)
    }

    @Test
    fun theTitleIsAHeadingAndTheNewTemplateButtonOpensTheEditor() {
        composeRule
            .onNodeWithText("Templates")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithContentDescription("New template").performClick()

        assertEquals(listOf<Route>(Route.Editor()), navigated)
    }

    @Test
    fun theAddRowEndsTheListAndOpensANewTemplate() {
        composeRule.onNodeWithText("Add new template").assertExists().assertHasClickAction()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithText("Add new template").performClick()

        assertEquals(listOf<Route>(Route.Editor()), navigated)
    }

    @Test
    fun theHeaderButtonAndTheAddRowAreBothFocusableButtons() {
        val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule
            .onNodeWithContentDescription(
                "New template",
            ).assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("Add new template").assert(button)
    }

    @Test
    fun settingsAndTheStartButtonsLiveOnTheHubNotHere() {
        composeRule.onNodeWithContentDescription("Settings").assertDoesNotExist()
        composeRule.onNodeWithText("deaftly").assertDoesNotExist()
        composeRule.onNodeWithText("Start typing").assertDoesNotExist()
    }

    @Test
    fun theHomeButtonReturnsToTheHub() {
        composeRule.onNodeWithText("Home").performClick()

        assertEquals(1, homes)
        assertTrue(navigated.isEmpty())
    }

    private fun templateInRepository(id: String) = repository.snapshot.first { it.id == id }

    private fun longClick(title: String) {
        composeRule.onAllNodesWithText(title)[0].performTouchInput { longClick() }
        composeRule.waitForIdle()
    }

    private fun invokeCustomAction(
        title: String,
        label: String,
    ) {
        val actions = composeRule.onAllNodesWithText(title)[0].fetchSemanticsNode().config[SemanticsActions.CustomActions]
        composeRule.runOnUiThread { actions.first { it.label == label }.action() }
        composeRule.waitForIdle()
    }

    @Test
    fun longPressOpensTheOptionsSheetForACard() {
        longClick("Explain I'm deaf")

        composeRule.onNodeWithText("Pin").assertExists()
        composeRule.onNodeWithText("Category").assertExists()
        composeRule.onNodeWithText("New category\u2026").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theSheetMarksTheCurrentCategory() {
        longClick("Explain I'm deaf")

        composeRule
            .onAllNodesWithText("Everyday")
            .filter(isSelectable())
            .onLast()
            .assertIsSelected()
    }

    @Test
    fun pinningFromTheSheetPersistsAndAddsATile() {
        longClick("Explain I'm deaf")
        composeRule.onNodeWithText("Pin").performClick()
        composeRule.waitForIdle()

        assertTrue(templateInRepository("explain-deaf").pinned)
        assertEquals(2, count("Explain I'm deaf"))
        composeRule.onNodeWithText("Pin").assertDoesNotExist()
    }

    @Test
    fun unpinningFromAPinnedTileRemovesItFromTheTiles() {
        longClick("Check in")
        composeRule.onNodeWithText("Unpin").performClick()
        composeRule.waitForIdle()

        assertFalse(templateInRepository("check-in").pinned)
        assertEquals(1, count("Check in"))
    }

    @Test
    fun choosingAnExistingCategoryPersists() {
        longClick("Explain I'm deaf")
        composeRule
            .onAllNodesWithText("Health")
            .filter(isSelectable())
            .onLast()
            .performClick()
        composeRule.waitForIdle()

        assertEquals("Health", templateInRepository("explain-deaf").category)
    }

    @Test
    fun aNewCategoryCanBeTypedAndSaved() {
        longClick("Explain I'm deaf")
        composeRule.onNodeWithText("New category\u2026").performClick()
        composeRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("Travel")
        composeRule.onNodeWithText("Save category").performClick()
        composeRule.waitForIdle()

        assertEquals("Travel", templateInRepository("explain-deaf").category)
        composeRule.onNodeWithText("Travel").assertExists()
    }

    @Test
    fun saveCategoryIsDisabledUntilANameIsTyped() {
        longClick("Explain I'm deaf")
        composeRule.onNodeWithText("New category\u2026").performClick()

        composeRule.onNodeWithText("Save category").assertIsNotEnabled()
    }

    @Test
    fun theCustomActionsPinAndUnpinWithoutTheSheet() {
        invokeCustomAction("Explain I'm deaf", "Pin")
        assertTrue(templateInRepository("explain-deaf").pinned)

        invokeCustomAction("Explain I'm deaf", "Unpin")
        assertFalse(templateInRepository("explain-deaf").pinned)
        composeRule.onNodeWithText("Category").assertDoesNotExist()
    }

    @Test
    fun theSheetOffersEditWhichOpensTheEditor() {
        longClick("Explain I'm deaf")

        composeRule.onNodeWithText("Edit template").performClick()

        assertEquals(listOf<Route>(Route.Editor("explain-deaf")), navigated)
        composeRule.onNodeWithText("Edit template").assertDoesNotExist()
    }

    @Test
    fun theSheetOffersDeleteWhichAsksFirstAndThenRemovesTheTemplate() {
        longClick("Explain I'm deaf")

        composeRule.onNodeWithText("Delete template").performClick()
        composeRule.onNodeWithText("Delete this template?").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        assertTrue(repository.snapshot.any { it.id == "explain-deaf" })

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        assertTrue(repository.snapshot.none { it.id == "explain-deaf" })
        composeRule.onNodeWithText("Explain I'm deaf").assertDoesNotExist()
    }

    @Test
    fun cancellingTheDeleteConfirmationKeepsTheTemplate() {
        longClick("Explain I'm deaf")
        composeRule.onNodeWithText("Delete template").performClick()

        composeRule.onNodeWithText("Cancel").performClick()

        assertTrue(repository.snapshot.any { it.id == "explain-deaf" })
        composeRule.onNodeWithText("Delete this template?").assertDoesNotExist()
    }

    @Test
    fun editAndDeleteAreCustomActionsForTalkBackAndDeleteAlsoConfirms() {
        invokeCustomAction("Explain I'm deaf", "Edit template")
        assertEquals(listOf<Route>(Route.Editor("explain-deaf")), navigated)

        invokeCustomAction("Explain I'm deaf", "Delete template")

        composeRule.onNodeWithText("Delete this template?").assertExists()
        assertTrue(repository.snapshot.any { it.id == "explain-deaf" })
    }

    @Test
    fun theSetCategoryCustomActionOpensTheSheet() {
        invokeCustomAction("At the doctor's", "Set category")

        composeRule.onNodeWithText("Category").assertExists()
    }

    @Test
    fun pinnedTilesExposeTheCustomActionsToo() {
        invokeCustomAction("Check in", "Unpin")

        assertFalse(templateInRepository("check-in").pinned)
    }
}
