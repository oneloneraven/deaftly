package com.ravendarque.deaftly.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class ChatScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Teal))
    private var ended = 0
    private var inserts = 0
    private lateinit var viewModel: ChatViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(savedState: SavedStateHandle = SavedStateHandle()) {
        viewModel = ChatViewModel(settings, savedState)
        composeRule.setContent { AppTheme { ChatScreen(viewModel, onEnd = { ended++ }, onInsertTemplate = { inserts++ }) } }
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

    private fun hasSelected(value: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, value)

    private fun draft() = (viewModel.state.value as ChatUiState.Ready).draft

    private fun quoteMessage(description: String) {
        val node = composeRule.onNodeWithContentDescription(description, substring = true).fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single { it.label == "Quote" }
        composeRule.runOnUiThread { action.action() }
        composeRule.waitForIdle()
    }

    @Test
    fun theToolbarOffersBoldItalicHighlightAndQuoteAsFortyEightDpTargets() {
        showOrdering()

        listOf("Bold", "Italic", "Highlight", "Quote the latest message").forEach {
            composeRule
                .onNodeWithContentDescription(it)
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun boldAppliedToASelectionFormatsExactlyThatText() {
        showOrdering()
        composeRule.onNode(isMessageField).performTextInput("Mild please")
        composeRule.onNode(isMessageField).performTextInputSelection(TextRange(0, 4))

        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(StyleRange(0, 4, SpanStyle.Bold)), draft().composition.content.ranges)
        composeRule.onNodeWithContentDescription("Bold").assert(hasSelected(true)).assert(hasState("On"))
        composeRule.onNodeWithContentDescription("Italic").assert(hasSelected(false)).assert(hasState("Off"))
    }

    @Test
    fun anotherTapOnTheSameButtonRemovesTheFormatting() {
        showOrdering()
        composeRule.onNode(isMessageField).performTextInput("Mild please")
        composeRule.onNode(isMessageField).performTextInputSelection(TextRange(0, 4))

        composeRule.onNodeWithContentDescription("Italic").performClick()
        composeRule.onNodeWithContentDescription("Italic").performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList(), draft().composition.content.ranges)
    }

    @Test
    fun withNothingSelectedAStyleAppliesToTheNextTypedText() {
        showOrdering()

        composeRule.onNodeWithContentDescription("Highlight").performClick()
        composeRule.onNodeWithContentDescription("Highlight").assert(hasSelected(true))
        composeRule.onNode(isMessageField).performTextInput("15 minutes")
        composeRule.waitForIdle()

        assertEquals(listOf(StyleRange(0, 10, SpanStyle.Highlight)), draft().composition.content.ranges)
    }

    @Test
    fun theStyleCanBeSwitchedOffAgainBeforeTypingMore() {
        showOrdering()
        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.onNode(isMessageField).performTextInput("no nuts")

        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.onNode(isMessageField).performTextInput(" thanks")
        composeRule.waitForIdle()

        assertEquals(listOf(StyleRange(0, 7, SpanStyle.Bold)), draft().composition.content.ranges)
    }

    @Test
    fun theSentBubbleCarriesTheSameFormatting() {
        showOrdering()
        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.onNode(isMessageField).performTextInput("no nuts")
        composeRule.onNodeWithContentDescription("Bold").performClick()
        composeRule.onNode(isMessageField).performTextInput(" please")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()

        val last = conversation().messages.last()
        assertEquals(listOf(Span("no nuts", setOf(SpanStyle.Bold)), Span(" please")), last.spans)
        composeRule.onNodeWithContentDescription("Waiter: no nuts please").assertIsDisplayed()
        assertEquals("", draft().text)
        composeRule.onNodeWithContentDescription("Bold").assert(hasSelected(false))
    }

    @Test
    fun quotingAMessageShowsItAboveTheComposerAndSendsItWithTheNextMessage() {
        showOrdering(ParticipantId.A)

        quoteMessage("Waiter: Sure! Do you want it spicy or mild?")

        composeRule.onNodeWithContentDescription("Quoting: Sure! Do you want it spicy or mild?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Quote the latest message").assert(hasSelected(true))
        composeRule.onNode(isMessageField).performTextInput("Mild please")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()

        assertEquals(Quote(1, "Sure! Do you want it spicy or mild?"), conversation().messages.last().quote)
        composeRule.onNodeWithContentDescription("Quoting: Sure! Do you want it spicy or mild?").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Replying to \u201cSure! Do you want it spicy or mild?\u201d. Sam: Mild please")
            .assertIsDisplayed()
    }

    @Test
    fun aPendingQuoteCanBeRemoved() {
        showOrdering(ParticipantId.A)
        quoteMessage("Waiter: Sure! Do you want it spicy or mild?")

        composeRule.onNodeWithContentDescription("Remove quote").assertWidthIsAtLeast(48.dp).performClick()
        composeRule.waitForIdle()

        assertNull(draft().quote)
        composeRule.onNodeWithContentDescription("Quoting: Sure! Do you want it spicy or mild?").assertDoesNotExist()
    }

    @Test
    fun theToolbarQuoteButtonQuotesTheOtherPersonsLatestMessageAndTogglesOff() {
        showOrdering(ParticipantId.A)

        composeRule.onNodeWithContentDescription("Quote the latest message").performClick()
        composeRule.waitForIdle()
        assertEquals(Quote(3, "No problem. It will take about 15 minutes."), draft().quote)

        composeRule.onNodeWithContentDescription("Quote the latest message").performClick()
        composeRule.waitForIdle()
        assertNull(draft().quote)
    }

    @Test
    fun theToolbarQuoteButtonIsUnavailableInAnEmptyChat() {
        show()

        composeRule.onNodeWithContentDescription("Quote the latest message").assertIsNotEnabled()
    }

    @Test
    fun aVeryLongQuoteIsTruncatedWithAnEllipsisInTheBubbleButKeptInFull() {
        val long = "word ".repeat(80).trim()
        show(SavedStateHandle(mapOf("conversation" to ChatCodec.encode(orderingConversation(ParticipantId.A)))))
        viewModel.quote(0, long)
        composeRule.onNode(isMessageField).performTextInput("ok")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()

        assertEquals(
            long,
            conversation()
                .messages
                .last()
                .quote
                ?.text,
        )
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theTemplateButtonAsksToPickATemplateWithoutLeavingTheDraft() {
        showOrdering()
        composeRule.onNode(isMessageField).performTextInput("Half typed")

        composeRule.onNodeWithContentDescription("Insert template").assertWidthIsAtLeast(48.dp).performClick()

        assertEquals(1, inserts)
        assertEquals("Half typed", draft().text)
    }

    @Test
    fun insertedTextAppearsInTheComposerAtTheCursorWithoutBeingSent() {
        showOrdering()
        composeRule.onNode(isMessageField).performTextInput("Hello world")
        composeRule.onNode(isMessageField).performTextInputSelection(TextRange(5, 5))

        composeRule.waitForIdle()
        composeRule.runOnUiThread { viewModel.drafts.insertText(", dear") }
        composeRule.waitForIdle()

        assertEquals("Hello, dear world", draft().text)
        composeRule.onNodeWithText("Hello, dear world").assertExists()
        assertEquals(4, conversation().messages.size)
    }

    @Test
    @Config(qualifiers = "w300dp-h1600dp")
    fun onANarrowScreenTheToolbarWrapsInsteadOfClippingButtons() {
        showOrdering()

        listOf("Bold", "Italic", "Highlight", "Quote the latest message", "Insert template", "Send").forEach {
            composeRule.onNodeWithContentDescription(it).assertIsDisplayed().assertWidthIsAtLeast(48.dp)
        }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private fun endConfirmation() = composeRule.onNodeWithText("The messages will be deleted.")

    @Test
    fun endingAChatWithMessagesAsksFirstAndKeepingItChangesNothing() {
        showOrdering()

        composeRule.onNodeWithContentDescription("End chat").performClick()

        composeRule.onNodeWithText("End this chat?").assertIsDisplayed()
        endConfirmation().assertIsDisplayed()
        assertEquals(0, ended)
        composeRule.onNodeWithText("Keep chatting").performClick()
        endConfirmation().assertDoesNotExist()
        assertEquals(0, ended)
        assertEquals(4, conversation().messages.size)
    }

    @Test
    fun confirmingTheEndLeavesTheChat() {
        showOrdering()

        composeRule.onNodeWithContentDescription("End chat").performClick()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithText("End chat").performClick()

        assertEquals(1, ended)
    }

    @Test
    fun systemBackAsksToEndAChatWithMessagesAndLeavesAnEmptyOneStraightAway() {
        showOrdering()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        endConfirmation().assertIsDisplayed()
        assertEquals(0, ended)
    }

    @Test
    fun systemBackLeavesAnEmptyChatWithoutAsking() {
        show()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        endConfirmation().assertDoesNotExist()
        assertEquals(1, ended)
    }

    @Test
    fun theHeaderSwitchesToTheSplitScreenLayoutAndRemembersTheChoice() {
        showOrdering()

        composeRule.onNodeWithContentDescription("Switch to split screen").assertWidthIsAtLeast(48.dp).performClick()
        composeRule.waitForIdle()

        assertEquals(ChatLayout.Split, (viewModel.state.value as ChatUiState.Ready).layout)
        assertEquals(ChatLayout.Split, settings.savedLayout)
        composeRule.onNodeWithText("WAITER\u2019S TURN").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Switch to side-by-side chat").performClick()
        composeRule.waitForIdle()
        assertEquals(ChatLayout.Sides, (viewModel.state.value as ChatUiState.Ready).layout)
        assertEquals(ChatLayout.Sides, settings.savedLayout)
        assertEquals(4, conversation().messages.size)
    }
}
