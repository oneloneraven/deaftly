package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.ParticipantId
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
    private lateinit var viewModel: ChatViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(savedState: SavedStateHandle = SavedStateHandle()) {
        viewModel = ChatViewModel(settings, savedState)
        composeRule.setContent { AppTheme { ChatScreen(viewModel, onEnd = { ended++ }) } }
        composeRule.waitForIdle()
    }

    private fun showOrdering(turn: ParticipantId = ParticipantId.B) =
        show(SavedStateHandle(mapOf("conversation" to ChatCodec.encode(orderingConversation(turn)))))

    private fun header(
        left: String = "Them",
        right: String = "Alex",
    ) = composeRule.onNodeWithContentDescription("$left and $right. Change names and colours")

    private fun turnControl() = composeRule.onNodeWithContentDescription("Whose turn")

    private fun tapSide(fraction: Float) = turnControl().performTouchInput { click(Offset(width * fraction, centerY)) }

    private fun tapLeft() = tapSide(0.25f)

    private fun tapRight() = tapSide(0.75f)

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun conversation() = (viewModel.state.value as ChatUiState.Ready).conversation

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

        composeRule.onAllNodes(isParticipantNameField)[1].performTextInput("Sam")

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

    @Test
    fun anEmptyChatSaysSoAndOffersTheDeafUserTheFirstTurn() {
        show()

        composeRule.onNodeWithText("No messages yet").assertIsDisplayed()
        composeRule.onNodeWithText("Type your message").assertIsDisplayed()
        turnControl().assert(hasState("Alex"))
    }

    @Test
    fun theConversationShowsEveryMessageWithItsAuthor() {
        showOrdering()

        composeRule.onNodeWithContentDescription("Sam: Hi! I'm deaf. Could I have pasta arrabbiata please?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Waiter: Sure! Do you want it spicy or mild?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sam: Mild please. And no nuts, I'm allergic.", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Waiter: No problem. It will take about 15 minutes.").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theHearingPersonsTurnPromptsThemToReply() {
        showOrdering(ParticipantId.B)

        composeRule.onNodeWithText("Your turn – type your reply here").assertIsDisplayed()
        turnControl().assert(hasState("Waiter"))
    }

    @Test
    fun theDeafUsersTurnShowsTheNeutralPromptInstead() {
        showOrdering(ParticipantId.A)

        composeRule.onNodeWithText("Type your message").assertIsDisplayed()
        composeRule.onNodeWithText("Your turn – type your reply here").assertDoesNotExist()
    }

    @Test
    fun tappingTheOtherSideSwitchesTheTurnAndThePrompt() {
        showOrdering(ParticipantId.A)

        tapLeft()
        composeRule.waitForIdle()

        assertEquals(ParticipantId.B, conversation().turn)
        composeRule.onNodeWithText("Your turn – type your reply here").assertIsDisplayed()
        turnControl().assert(hasState("Waiter"))
    }

    @Test
    fun tappingTheActiveSideDoesNothing() {
        showOrdering(ParticipantId.B)

        tapLeft()

        assertEquals(ParticipantId.B, conversation().turn)
    }

    @Test
    fun swipingTheControlSwitchesTurnsInBothDirections() {
        showOrdering(ParticipantId.B)

        turnControl().performTouchInput { swipeRight(startX = centerX - width / 4, endX = centerX + width / 3) }
        composeRule.waitForIdle()
        assertEquals(ParticipantId.A, conversation().turn)

        turnControl().performTouchInput { swipeLeft(startX = centerX + width / 4, endX = centerX - width / 3) }
        composeRule.waitForIdle()
        assertEquals(ParticipantId.B, conversation().turn)
    }

    @Test
    fun aTinySwipeSnapsBack() {
        showOrdering(ParticipantId.B)

        turnControl().performTouchInput { swipeRight(startX = centerX - width / 4, endX = centerX - width / 4 + width / 12) }
        composeRule.waitForIdle()

        assertEquals(ParticipantId.B, conversation().turn)
    }

    @Test
    fun theControlCanBeOperatedWithoutGesturesThroughItsClickAction() {
        showOrdering(ParticipantId.B)

        turnControl().performClick()
        composeRule.waitForIdle()

        assertEquals(ParticipantId.A, conversation().turn)
    }

    @Test
    fun sendingAddsTheMessageOnThatPersonsSideWithoutChangingTurn() {
        showOrdering(ParticipantId.B)
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction()).performTextInput("Thanks, that is fine")
        composeRule.onNodeWithContentDescription("Send").assertIsEnabled().performClick()
        composeRule.waitForIdle()

        val last = conversation().messages.last()
        assertEquals("Thanks, that is fine", last.plainText)
        assertEquals(ParticipantId.B, last.author)
        assertEquals(ParticipantId.B, conversation().turn)
        composeRule.onNodeWithContentDescription("Waiter: Thanks, that is fine").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun aDraftStaysWithItsPersonWhenTheTurnSwitchesAndComesBack() {
        show()
        composeRule.onNode(hasSetTextAction()).performTextInput("half a thought")

        tapLeft()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("half a thought").assertDoesNotExist()
        tapRight()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("half a thought").assertIsDisplayed()
    }

    @Test
    fun theTextSizeButtonCyclesThroughThreeSizes() {
        show()
        val button = { composeRule.onNodeWithContentDescription("Change text size") }

        button().assert(hasState("Normal")).performClick()
        button().assert(hasState("Large")).performClick()
        button().assert(hasState("Largest")).performClick()
        button().assert(hasState("Normal"))
    }
}
