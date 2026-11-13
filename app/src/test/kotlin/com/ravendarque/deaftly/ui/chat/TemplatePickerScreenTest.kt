package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
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
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import com.ravendarque.deaftly.ui.home.HomeViewModel
import com.ravendarque.deaftly.ui.home.homeTemplates
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
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class TemplatePickerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val filled = mutableListOf<String>()
    private val inserted = mutableListOf<String>()
    private var backs = 0

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        val viewModel = HomeViewModel(FakeTemplateRepository(homeTemplates))
        composeRule.setContent {
            AppTheme { TemplatePickerScreen(viewModel, onFill = { filled += it }, onInsert = { inserted += it }, onBack = { backs++ }) }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun listsTheTemplatesWithASearchField() {
        composeRule.onNodeWithText("Insert template").assertIsDisplayed()
        composeRule.onNodeWithText("Search templates").assertIsDisplayed()
        composeRule.onAllNodesWithText("Explain I'm deaf")[0].assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aTemplateWithPlaceholdersGoesToTheFillFlow() {
        composeRule.onAllNodesWithText("Explain I'm deaf")[0].performClick()

        assertEquals(listOf("explain-deaf"), filled)
        assertEquals(emptyList(), inserted)
    }

    @Test
    fun aTemplateWithoutPlaceholdersIsInsertedStraightAway() {
        composeRule.onAllNodesWithText("At the doctor's")[0].performClick()

        assertEquals(listOf("I'm deaf. Please write down what the doctor says."), inserted)
        assertEquals(emptyList(), filled)
    }

    @Test
    fun searchNarrowsTheList() {
        composeRule.onNodeWithText("Search templates").performTextInput("doctor")
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithText("Explain I'm deaf").fetchSemanticsNodes().size)
        composeRule.onAllNodesWithText("At the doctor's")[0].assertIsDisplayed()
    }

    @Test
    fun backLeavesThePicker() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
