package com.ravendarque.deaftly.ui.home

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigated = mutableListOf<Route>()

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        val viewModel = HomeViewModel(FakeTemplateRepository(homeTemplates))
        composeRule.setContent { AppTheme { HomeScreen(viewModel, onNavigate = { navigated += it }) } }
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
    fun headerButtonsOpenTheEditorAndSettings() {
        composeRule.onNodeWithContentDescription("New template").performClick()
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(listOf<Route>(Route.Editor, Route.Settings), navigated)
    }

    @Test
    fun dockButtonsOpenShowAndChat() {
        composeRule.onNodeWithText("Type & show").performClick()
        composeRule.onNodeWithText("Start chat").performClick()

        assertEquals(listOf<Route>(Route.Show, Route.Chat), navigated)
    }
}
