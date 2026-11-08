package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
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
class ChatScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Teal))
    private var ended = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show() {
        val viewModel = ChatViewModel(settings, SavedStateHandle())
        composeRule.setContent { AppTheme { ChatScreen(viewModel, onEnd = { ended++ }) } }
        composeRule.waitForIdle()
    }

    private fun header(
        left: String = "Them",
        right: String = "Alex",
    ) = composeRule.onNodeWithContentDescription("$left and $right. Change names and colours")

    @Test
    fun theHeaderShowsBothPeopleWithTheSavedNameForTheDeafUser() {
        show()

        header().assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun tappingTheHeaderOpensTheSheet() {
        show()

        header().performClick()

        composeRule.onNodeWithText("Who is chatting?").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun namingTheOtherPersonUpdatesTheHeaderStraightAway() {
        show()
        header().performClick()

        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("Sam")

        header(left = "Sam").assertExists()
    }

    @Test
    fun finishingTheSheetRemembersTheDeafUsersColour() {
        show()
        header().performClick()

        composeRule.onAllNodesWithContentDescription("Rose")[0].performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()

        assertEquals(ChatProfile("Alex", ColourRole.Rose), settings.saved)
        composeRule.onNodeWithText("Who is chatting?").assertDoesNotExist()
    }

    @Test
    fun endingTheChatIsReported() {
        show()

        composeRule.onNodeWithContentDescription("End chat").performClick()

        assertEquals(1, ended)
    }
}
