package com.ravendarque.deaftly.ui.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class TemplatesEmptyScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigated = mutableListOf<Route>()

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        val viewModel = HomeViewModel(FakeTemplateRepository(emptyList()))
        composeRule.setContent { AppTheme { TemplatesScreen(viewModel, onNavigate = { navigated += it }, onHome = {}) } }
        composeRule.waitForIdle()
    }

    @Test
    fun anEmptyListStillOffersTheAddRow() {
        composeRule.onNodeWithText("No templates yet").assertExists()
        composeRule.onNodeWithText("Add new template").assertExists().assertHasClickAction()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theAddRowAndTheHeaderButtonBothOpenANewTemplate() {
        composeRule.onNodeWithText("Add new template").performClick()
        composeRule.onNodeWithContentDescription("New template").performClick()

        assertEquals(listOf<Route>(Route.Editor(), Route.Editor()), navigated)
    }
}
