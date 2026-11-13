package com.ravendarque.deaftly.ui.typeshow

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class TypeShowScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableListOf<String>()
    private var backs = 0
    private val field = TextFieldState()

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun open() {
        composeRule.setContent {
            AppTheme { TypeShowContent(field, onShow = { shown += it }, onBack = { backs++ }) }
        }
        composeRule.waitForIdle()
    }

    private fun input() = composeRule.onNodeWithContentDescription("Message to show")

    @Test
    fun opensWithAnEmptyFocusedFieldAndShowAndClearDisabled() {
        open()

        input().assertIsFocused()
        composeRule.onNodeWithText("Type your message\u2026").assertExists()
        composeRule.onNodeWithText("Show").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Clear message").assertIsNotEnabled()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun showPassesTheTrimmedMessage() {
        open()

        input().performTextInput("  Table for two\nby the window  ")
        composeRule.onNodeWithText("Show").assertIsEnabled().performClick()

        assertEquals(listOf("Table for two\nby the window"), shown)
    }

    @Test
    fun aBlankMessageCannotBeShown() {
        open()

        input().performTextInput("   \n ")

        composeRule.onNodeWithText("Show").assertIsNotEnabled()
    }

    @Test
    fun clearEmptiesTheFieldForTheNextMessage() {
        open()
        input().performTextInput("Table for two")

        composeRule.onNodeWithContentDescription("Clear message").performClick()

        assertEquals("", field.text.toString())
        composeRule.onNodeWithText("Type your message\u2026").assertExists()
        composeRule.onNodeWithText("Show").assertIsNotEnabled()
    }

    @Test
    fun theCursorCanBeMovedAndTextInsertedInTheMiddleOfLines() {
        open()
        input().performTextInput("one\ntwo\nthree")

        input().performTextInputSelection(TextRange(4))
        input().performTextInput("2")

        assertEquals("one\n2two\nthree", field.text.toString())
    }

    @Test
    fun backNavigates() {
        open()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }
}
