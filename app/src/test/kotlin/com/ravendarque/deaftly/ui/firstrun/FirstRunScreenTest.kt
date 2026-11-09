package com.ravendarque.deaftly.ui.firstrun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.settings.ChatLayout
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
@Config(qualifiers = "w390dp-h844dp")
class FirstRunScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var state by mutableStateOf(FirstRunState())
    private var started = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(dark: Boolean = false) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                FirstRunContent(
                    state = state,
                    onName = { state = state.copy(name = it) },
                    onColour = { state = state.copy(colour = it) },
                    onLayout = { state = state.copy(layout = it) },
                    onGetStarted = { started++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun showsTheWelcomeTheControlsAndThePrivacyStatement() {
        show()

        composeRule.onNodeWithText("Welcome to Deaftly").assertExists()
        composeRule.onNodeWithText("Side by side").assertIsSelected()
        composeRule.onNodeWithText("Split screen").assertExists()
        composeRule.onNodeWithText("Deaftly has no internet access. Your templates and chats never leave your phone.").assertExists()
        composeRule.onNodeWithText("Get started").assertExists()
    }

    @Test
    fun passesAccessibilityChecksInTheLightTheme() {
        show()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun passesAccessibilityChecksInTheDarkTheme() {
        show(dark = true)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theControlsReportTheirChoices() {
        show()

        composeRule.onNode(hasSetTextAction()).performTextInput("Sam")
        composeRule.onNodeWithText("Split screen").performClick()

        assertEquals("Sam", state.name)
        assertEquals(ChatLayout.Split, state.layout)
    }

    @Test
    fun getStartedIsReported() {
        show()

        composeRule.onNodeWithText("Get started").performClick()

        assertEquals(1, started)
    }
}
