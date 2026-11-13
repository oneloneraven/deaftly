package com.ravendarque.deaftly.ui.chat

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class ChatScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green))
    private var ended = 0
    private var inserts = 0
    private val haptics = mutableListOf<HapticFeedbackType>()
    private val recordingHaptics =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType
            }
        }
    private lateinit var viewModel: ChatViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(savedState: SavedStateHandle = SavedStateHandle()) {
        viewModel = ChatViewModel(settings, savedState)
        composeRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recordingHaptics) {
                AppTheme { ChatScreen(viewModel, onEnd = { ended++ }, onInsertTemplate = { inserts++ }) }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showOrdering(turn: ParticipantId = ParticipantId.B) =
        show(SavedStateHandle(mapOf("conversation" to ChatCodec.encode(orderingConversation(turn)))))

    private fun setup() = composeRule.onNodeWithContentDescription("Names and colours")

    private fun turnControl() = composeRule.onNodeWithContentDescription("Whose turn")

    private fun tapName(name: String) = composeRule.onNodeWithText(name).performClick()

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun conversation() = (viewModel.state.value as ChatUiState.Ready).conversation

    @Test
    fun theHeaderOffersEndTextSizeSwitchSidesAndSetupAsFortyEightDpTargets() {
        show()

        listOf("End chat", "Change text size", "Switch sides", "Names and colours").forEach {
            composeRule
                .onNodeWithContentDescription(it)
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theHeaderNoLongerShowsTheParticipants() {
        show()

        composeRule.onNodeWithContentDescription("Them and Alex. Change names and colours").assertDoesNotExist()
    }

    @Test
    fun theCogOpensTheSetupSheet() {
        show()

        setup().performClick()

        composeRule.onNodeWithText("Who is chatting?").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun namingTheOtherPersonUpdatesTheConversation() {
        show()
        setup().performClick()

        composeRule.onAllNodes(isParticipantNameField)[1].performTextInput("Sam")

        assertEquals("Sam", conversation().participantB.name)
    }

    @Test
    fun finishingTheSheetRemembersTheDeafUsersColour() {
        show()
        setup().performClick()

        composeRule.onAllNodesWithContentDescription("Red")[0].performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()

        assertEquals(ChatProfile("Alex", ColourRole.Red), settings.saved)
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
        turnControl().assert(hasState("Alex\u2019s turn"))
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

    private fun nameLabel(
        name: String,
        index: Int = 0,
    ) = composeRule.onAllNodesWithText(name.uppercase(), useUnmergedTree = true)[index]

    private fun isOnRight(name: String): Boolean {
        val rootWidth =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.width
        return nameLabel(name)
            .fetchSemanticsNode()
            .boundsInRoot.center.x > rootWidth / 2
    }

    @Test
    fun eachMessageIsAScriptLineWithAnUppercaseNameLabelOnTheSpeakersSide() {
        showOrdering()

        assertEquals(true, isOnRight("Sam"))
        assertEquals(false, isOnRight("Waiter"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun switchingSidesMirrorsTheScript() {
        showOrdering()

        composeRule.onNodeWithContentDescription("Switch sides").performClick()
        composeRule.waitForIdle()

        assertEquals(false, isOnRight("Sam"))
        assertEquals(true, isOnRight("Waiter"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun switchedSidesAreRememberedByTheNextChat() {
        settings.apply { runBlocking { setChatSidesSwapped(true) } }
        showOrdering()

        assertEquals(false, isOnRight("Sam"))
    }

    @Test
    fun aQuoteShowsAsAMutedLineAboveTheMessageText() {
        showOrdering()

        composeRule.onAllNodesWithText("spicy or mild?", useUnmergedTree = true).assertCountEquals(1)
        composeRule
            .onNodeWithContentDescription("Replying to \u201cspicy or mild?\u201d. Sam: Mild please. And no nuts, I'm allergic.")
            .assertIsDisplayed()
    }

    @Test
    fun theHearingPersonsPromptShowsUntilTheyHaveSent() {
        show(SavedStateHandle(mapOf("conversation" to ChatCodec.encode(deafUserHasSpoken()))))

        composeRule.onNodeWithText("Your turn – type your reply here").assertIsDisplayed()
        turnControl().assert(hasState("Waiter\u2019s turn"))
    }

    @Test
    fun theHearingPersonsPromptIsGoneOnceTheyHaveSent() {
        showOrdering(ParticipantId.B)

        composeRule.onNodeWithText("Your turn – type your reply here").assertDoesNotExist()
        turnControl().assert(hasState("Waiter\u2019s turn"))
    }

    @Test
    fun theDeafUsersPromptGoesOnceTheyHaveSent() {
        showOrdering(ParticipantId.A)

        composeRule.onNodeWithText("Type your message").assertDoesNotExist()
        composeRule.onNodeWithText("Your turn – type your reply here").assertDoesNotExist()
    }

    @Test
    fun sendingTheFirstMessageHidesThatPersonsPromptAndLeavesTheOthersAlone() {
        show()
        composeRule.onNodeWithText("Type your message").assertIsDisplayed()

        composeRule.onNode(isMessageField).performTextInput("Hello")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Type your message").assertDoesNotExist()

        composeRule.runOnUiThread { viewModel.selectTurn(ParticipantId.B) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your turn – type your reply here").assertIsDisplayed()

        composeRule.onNode(isMessageField).performTextInput("Hi")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your turn – type your reply here").assertDoesNotExist()
    }

    @Test
    fun tappingTheOtherSideSwitchesTheTurnAndAnnouncesIt() {
        showOrdering(ParticipantId.A)

        tapName("Waiter")
        composeRule.waitForIdle()

        assertEquals(ParticipantId.B, conversation().turn)
        turnControl().assert(hasState("Waiter\u2019s turn"))
    }

    @Test
    fun tappingTheActiveSideDoesNothing() {
        showOrdering(ParticipantId.B)

        tapName("Waiter")

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

        turnControl().performTouchInput { swipeRight(startX = centerX - width / 4, endX = centerX - width / 4 + width / 6) }
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

    private fun centreX(text: String) =
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .boundsInRoot.center.x

    private fun rootCentreX() =
        composeRule
            .onRoot()
            .fetchSemanticsNode()
            .boundsInRoot.center.x

    @Test
    fun theSwitchSitsBetweenTheNamesWithTheOtherPersonOnTheLeftAndTheDeafUserOnTheRight() {
        showOrdering()

        assertTrue(centreX("Waiter") < rootCentreX())
        assertTrue(centreX("Sam") > rootCentreX())
        composeRule.onNodeWithText("Swipe to Waiter").assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun switchingSidesMirrorsTheNamesAroundTheSwitch() {
        showOrdering()

        composeRule.onNodeWithContentDescription("Switch sides").performClick()
        composeRule.waitForIdle()

        assertTrue(centreX("Waiter") > rootCentreX())
        assertTrue(centreX("Sam") < rootCentreX())
    }

    @Test
    fun theActiveNameIsMarkedSelectedAndTheOtherIsNot() {
        showOrdering(ParticipantId.B)

        composeRule.onNodeWithText("Waiter").assert(hasSelected(true))
        composeRule.onNodeWithText("Sam").assert(hasSelected(false))
    }

    @Test
    fun theNamesAreFortyEightDpTargets() {
        showOrdering()

        listOf("Waiter", "Sam").forEach { composeRule.onNodeWithText(it).assertHeightIsAtLeast(48.dp) }
        turnControl().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(96.dp)
    }

    @Test
    fun theTurnIsAnnouncedPolitelyAsTheNamesTurn() {
        showOrdering(ParticipantId.A)

        turnControl().assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        tapName("Waiter")
        composeRule.waitForIdle()
        turnControl().assert(hasState("Waiter\u2019s turn"))
    }

    @Test
    fun aLongNameIsCutShortButTheFullNameIsStillReadOut() {
        val long = "A very long name indeed for a waiter"
        val ordering = orderingConversation(ParticipantId.B)
        val renamed = ordering.withParticipant(ordering.participantB.copy(name = long))
        show(SavedStateHandle(mapOf("conversation" to ChatCodec.encode(renamed))))

        composeRule.onNodeWithText(long).assertExists()
        turnControl().assert(hasState("$long\u2019s turn"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun everySwitchOfTurnGivesAHapticTick() {
        showOrdering(ParticipantId.B)

        tapName("Sam")
        tapName("Waiter")
        turnControl().performTouchInput { swipeRight(startX = centerX - width / 4, endX = centerX + width / 3) }
        composeRule.waitForIdle()

        assertEquals(List(3) { HapticFeedbackType.SegmentTick }, haptics)
    }

    @Test
    fun tappingTheActiveNameGivesNoHapticTick() {
        showOrdering(ParticipantId.B)

        tapName("Waiter")

        assertEquals(emptyList(), haptics)
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

        tapName("Them")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("half a thought").assertDoesNotExist()
        tapName("Alex")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("half a thought").assertIsDisplayed()
    }

    @Test
    fun theTextSizeButtonCyclesThroughThreeSizesStartingAtMedium() {
        show()
        val button = { composeRule.onNodeWithContentDescription("Change text size") }

        button().assert(hasState("Medium")).performClick()
        button().assert(hasState("Large")).performClick()
        button().assert(hasState("Small")).performClick()
        button().assert(hasState("Medium"))
    }

    @Test
    fun theTextSizeIsSavedToSettings() {
        show()

        composeRule.onNodeWithContentDescription("Change text size").performClick()
        composeRule.waitForIdle()

        assertEquals(ChatTextSize.Large, runBlocking { settings.chatTextSize.first() })
    }

    @Test
    fun switchSidesIsSavedToSettings() {
        show()

        composeRule.onNodeWithContentDescription("Switch sides").performClick()
        composeRule.waitForIdle()

        assertEquals(true, runBlocking { settings.chatSidesSwapped.first() })
    }

    private fun hasSelected(value: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, value)

    private fun draft() = (viewModel.state.value as ChatUiState.Ready).draft

    private fun quoteMessage(description: String) {
        val node = composeRule.onNodeWithContentDescription(description, substring = true).fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single { it.label == "Quote" }
        composeRule.runOnUiThread { action.action() }
        composeRule.waitForIdle()
    }

    private fun longPress(description: String) {
        composeRule
            .onNodeWithContentDescription(description, substring = true)
            .performTouchInput { longClick() }
        composeRule.waitForIdle()
    }

    private fun clipboardText(): String? {
        val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(ClipboardManager::class.java)
        return manager.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString()
    }

    @Test
    fun longPressingAMessageOffersCopyQuoteAndDelete() {
        showOrdering(ParticipantId.A)

        longPress("Waiter: Sure! Do you want it spicy or mild?")

        listOf("Copy", "Quote", "Delete").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun copyPutsTheMessagesPlainTextOnTheClipboard() {
        showOrdering(ParticipantId.A)
        longPress("Sam: Mild please. And no nuts, I'm allergic.")

        composeRule.onNodeWithText("Copy").performClick()
        composeRule.waitForIdle()

        assertEquals("Mild please. And no nuts, I'm allergic.", clipboardText())
        composeRule.onNodeWithText("Copy").assertDoesNotExist()
    }

    @Test
    fun quoteFromTheMenuReplacesAnyExistingPendingQuote() {
        showOrdering(ParticipantId.A)
        quoteMessage("Waiter: Sure! Do you want it spicy or mild?")

        longPress("Waiter: No problem. It will take about 15 minutes.")
        composeRule.onNodeWithText("Quote").performClick()
        composeRule.waitForIdle()

        assertEquals(Quote(3, "No problem. It will take about 15 minutes."), draft().quote)
        composeRule.onNodeWithContentDescription("Quoting: No problem. It will take about 15 minutes.").assertIsDisplayed()
    }

    @Test
    fun deleteFromTheMenuRemovesTheMessageStraightAway() {
        showOrdering(ParticipantId.A)
        longPress("Waiter: Sure! Do you want it spicy or mild?")

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        assertEquals(3, conversation().messages.size)
        composeRule.onNodeWithContentDescription("Waiter: Sure! Do you want it spicy or mild?", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test
    fun theMessageActionsAreAlsoCustomActionsForTalkBack() {
        showOrdering(ParticipantId.A)
        val node =
            composeRule
                .onNodeWithContentDescription(
                    "Waiter: Sure! Do you want it spicy or mild?",
                    substring = true,
                ).fetchSemanticsNode()
        val labels = node.config[SemanticsActions.CustomActions].map { it.label }

        assertEquals(listOf("Copy", "Quote", "Delete"), labels)
        val delete = node.config[SemanticsActions.CustomActions].single { it.label == "Delete" }
        composeRule.runOnUiThread { delete.action() }
        composeRule.waitForIdle()
        assertEquals(3, conversation().messages.size)
    }

    @Test
    fun theToolbarOffersBoldItalicAndHighlightAsFortyEightDpTargets() {
        showOrdering()

        listOf("Bold", "Italic", "Highlight").forEach {
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
    fun theToolbarHasNoQuoteButtonAndAPendingQuoteCanBeRemovedFromItsPreview() {
        showOrdering(ParticipantId.A)
        composeRule.onNodeWithContentDescription("Quote the latest message").assertDoesNotExist()

        quoteMessage("Waiter: Sure! Do you want it spicy or mild?")
        composeRule.onNodeWithContentDescription("Remove quote").performClick()
        composeRule.waitForIdle()

        assertNull(draft().quote)
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
    @Config(qualifiers = "en-rGB-w300dp-h1600dp")
    fun onANarrowScreenTheToolbarWrapsInsteadOfClippingButtons() {
        showOrdering()

        listOf("Bold", "Italic", "Highlight", "Insert template", "Send").forEach {
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
}
