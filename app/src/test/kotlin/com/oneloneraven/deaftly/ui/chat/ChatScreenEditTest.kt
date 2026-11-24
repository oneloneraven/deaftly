package com.oneloneraven.deaftly.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oneloneraven.deaftly.core.chat.ChatProfile
import com.oneloneraven.deaftly.core.chat.ColourRole
import com.oneloneraven.deaftly.core.chat.Composition
import com.oneloneraven.deaftly.core.chat.Conversation
import com.oneloneraven.deaftly.core.chat.ParticipantId
import com.oneloneraven.deaftly.core.chat.RichText
import com.oneloneraven.deaftly.ui.permissions.PermissionAccess
import com.oneloneraven.deaftly.ui.permissions.RuntimePermission
import com.oneloneraven.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
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
class ChatScreenEditTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: ChatViewModel

    private val ownMessage = "Sam: Mild please. And no nuts, I'm allergic."
    private val theirMessage = "Waiter: No problem. It will take about 15 minutes."

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(conversation: Conversation) {
        viewModel =
            ChatViewModel(
                FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green)),
                SavedStateHandle(mapOf("conversation" to ChatCodec.encode(conversation))),
                store = FakeSavedChatStore(),
                writeScope = testWriteScope,
                createSpeechRecogniser = ::silentRecogniser,
                audio = silentAudio(),
                speechWork = Dispatchers.Unconfined,
            )
        composeRule.setContent {
            AppTheme {
                ChatScreen(
                    viewModel,
                    onEnd = {},
                    onInsertTemplate = {},
                    microphone = RuntimePermission(PermissionAccess.Granted, {}, {}),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun conversation() = (viewModel.state.value as ChatUiState.Ready).conversation

    private fun draft() = (viewModel.state.value as ChatUiState.Ready).draft

    private fun message(description: String) = composeRule.onNodeWithContentDescription(description, substring = true)

    private fun actions(description: String) =
        message(description)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { it.label }

    @Test
    fun tappingYourOwnMessageOnYourTurnShowsTheEditingBarAndLoadsTheText() {
        show(orderingConversation(ParticipantId.A))

        message(ownMessage).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Editing message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cancel editing").assertIsDisplayed()
        composeRule.onNode(isMessageField).assertTextContains("Mild please. And no nuts, I'm allergic.")
        message(ownMessage).assertIsDisplayed()
        assertEquals(4, conversation().messages.size)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun tappingTheOtherPersonsMessageDoesNothing() {
        show(orderingConversation(ParticipantId.A))

        message(theirMessage).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Editing message").assertDoesNotExist()
        assertEquals(null, draft().editing)
    }

    @Test
    fun tappingTheOtherPersonsMessageShowsANoticeThatFades() {
        show(orderingConversation(ParticipantId.A))
        composeRule.mainClock.autoAdvance = false

        message(theirMessage).performClick()
        composeRule.mainClock.advanceTimeBy(FADE_MILLIS)

        composeRule.onNodeWithText("You can only edit your own messages").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(NOTICE_MILLIS + FADE_MILLIS)

        composeRule.onNodeWithText("You can only edit your own messages").assertDoesNotExist()
    }

    @Test
    fun tappingYourOwnMessageOnYourTurnShowsNoNotice() {
        show(orderingConversation(ParticipantId.A))

        message(ownMessage).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("You can only edit your own messages").assertDoesNotExist()
    }

    @Test
    fun tappingYourOwnMessageOnTheOtherPersonsTurnDoesNothing() {
        show(orderingConversation(ParticipantId.B))

        message(ownMessage).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Editing message").assertDoesNotExist()
    }

    @Test
    fun editIsInTheMenuAndTalkBackActionsOnlyForYourOwnMessagesOnYourTurn() {
        show(orderingConversation(ParticipantId.A))

        assertEquals(listOf("Edit", "Copy", "Quote", "Delete"), actions(ownMessage))
        assertEquals(listOf("Copy", "Quote", "Delete"), actions(theirMessage))

        message(theirMessage).performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Copy").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun editFromTheLongPressMenuStartsEditing() {
        show(orderingConversation(ParticipantId.A))

        message(ownMessage).performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Edit").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Editing message").assertIsDisplayed()
    }

    @Test
    fun editIsNotOfferedOnTheOtherPersonsTurn() {
        show(orderingConversation(ParticipantId.B))

        assertEquals(listOf("Copy", "Quote", "Delete"), actions(ownMessage))
        assertEquals(listOf("Edit", "Copy", "Quote", "Delete"), actions(theirMessage))
    }

    @Test
    fun cancellingRemovesTheBarAndRestoresTheDraft() {
        show(orderingConversation(ParticipantId.A))
        viewModel.drafts.setComposition(Composition().withText("Half typed", 10, 10))
        composeRule.waitForIdle()

        message(ownMessage).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Cancel editing").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Editing message").assertDoesNotExist()
        composeRule.onNode(isMessageField).assertTextContains("Half typed")
        assertEquals(4, conversation().messages.size)
    }

    @Test
    fun sendingTheEditShowsEditedAndKeepsTheMessageInPlace() {
        show(orderingConversation(ParticipantId.A))

        message(ownMessage).performClick()
        composeRule.waitForIdle()
        viewModel.drafts.setComposition(Composition().withText("Medium please", 13, 13))
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Editing message").assertDoesNotExist()
        composeRule.onNodeWithText("EDITED").assertIsDisplayed()
        message("Sam: Medium please, edited").assertIsDisplayed()
        assertEquals(listOf(0, 1, 2, 3), conversation().messages.map { it.id })
    }

    @Test
    fun anEditedMessageShowsTheLabelAndSaysSoToTalkBack() {
        val edited = orderingConversation(ParticipantId.A).replaceMessage(3, RichText.spans("Ten minutes."), null)
        show(edited)

        composeRule.onNodeWithText("EDITED").assertIsDisplayed()
        message("Waiter: Ten minutes., edited").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theMicIsDisabledWhileEditing() {
        show(orderingConversation(ParticipantId.B))
        composeRule.onNodeWithContentDescription("Start voice typing").performClick()

        message(theirMessage).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Start voice typing").assertIsNotEnabled()
        assertEquals(false, viewModel.voice.state.value.on)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}

private const val FADE_MILLIS = 500L
