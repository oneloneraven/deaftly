package com.ravendarque.deaftly.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.speech.AudioSource
import com.ravendarque.deaftly.core.speech.FakeSpeechRecogniser
import com.ravendarque.deaftly.core.speech.SpeechEvent
import com.ravendarque.deaftly.core.speech.SpeechRecogniser
import com.ravendarque.deaftly.ui.permissions.PermissionAccess
import com.ravendarque.deaftly.ui.permissions.RuntimePermission
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class ChatScreenVoiceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green))
    private var ended = 0
    private var microphoneAccess by mutableStateOf(PermissionAccess.Granted)
    private var permissionRequests = 0
    private var settingsOpened = 0
    private lateinit var viewModel: ChatViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(
        conversation: Conversation? = null,
        createSpeechRecogniser: () -> SpeechRecogniser = ::silentRecogniser,
        audio: AudioSource = silentAudio(),
    ) {
        viewModel =
            ChatViewModel(
                settings,
                SavedStateHandle(listOfNotNull(conversation?.let { "conversation" to ChatCodec.encode(it) }).toMap()),
                store = FakeSavedChatStore(),
                writeScope = testWriteScope,
                createSpeechRecogniser = createSpeechRecogniser,
                audio = audio,
                speechWork = Dispatchers.Unconfined,
            )
        composeRule.setContent {
            AppTheme {
                ChatScreen(
                    viewModel,
                    onEnd = { ended++ },
                    onInsertTemplate = {},
                    microphone = RuntimePermission(microphoneAccess, { permissionRequests++ }, { settingsOpened++ }),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showListening(
        speech: List<SpeechEvent?> = listOf(SpeechEvent.Partial("Tuesday works")),
        conversation: Conversation = orderingConversation(ParticipantId.B),
    ) = show(conversation, createSpeechRecogniser = { FakeSpeechRecogniser(speech) }, audio = HangingAudio(1))

    private fun startVoiceTyping() {
        composeRule.onNodeWithContentDescription("Start voice typing").performClick()
        composeRule.waitForIdle()
    }

    private fun showOrdering(turn: ParticipantId) = show(orderingConversation(turn))

    private fun tapName(name: String) = composeRule.onNodeWithText(name).performClick()

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun conversation() = (viewModel.state.value as ChatUiState.Ready).conversation

    private fun draft() = (viewModel.state.value as ChatUiState.Ready).draft

    private fun longPress(description: String) {
        composeRule
            .onNodeWithContentDescription(description, substring = true)
            .performTouchInput { longClick() }
        composeRule.waitForIdle()
    }

    private val hasPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun voiceTypedConversation() = orderingConversation(ParticipantId.B).appendVoiceTyped("Thursday")

    @Test
    fun micShowsOnlyOnTheHearingPersonsTurn() {
        showOrdering(ParticipantId.A)
        composeRule.onNodeWithContentDescription("Start voice typing").assertDoesNotExist()

        tapName("Waiter")
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Start voice typing")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun voiceOnHidesTheFieldAndDisablesTheOtherButtons() {
        showListening()
        composeRule.onNode(isMessageField).assertIsDisplayed()

        startVoiceTyping()

        composeRule.onNode(isMessageField).assertDoesNotExist()
        listOf("Send", "Bold", "Italic", "Highlight", "Insert template").forEach {
            composeRule.onNodeWithContentDescription(it).assertIsNotEnabled()
        }
        composeRule
            .onNodeWithContentDescription("Stop voice typing")
            .assertIsEnabled()
            .assert(hasState("On"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun stoppingVoiceTypingBringsTheFieldBack() {
        showListening(speech = emptyList())
        startVoiceTyping()

        composeRule.onNodeWithContentDescription("Stop voice typing").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(isMessageField).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start voice typing").assert(hasState("Off"))
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun voiceOnShowsTheStatusLine() {
        showListening()

        startVoiceTyping()

        composeRule
            .onNodeWithText("Voice typing is on. Speak clearly towards the device.")
            .assertIsDisplayed()
        composeRule
            .onNode(hasAnyDescendant(hasText("Voice typing is on. Speak clearly towards the device.")) and hasPoliteLiveRegion)
            .assertExists()
    }

    @Test
    fun liveTextShowsInTheMessageList() {
        showListening()

        startVoiceTyping()

        composeRule
            .onNodeWithContentDescription("Waiter: Tuesday works")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        assertEquals(4, conversation().messages.size)
    }

    @Test
    fun tappingAVoiceMessageEditsItOnTheHearingPersonsTurn() {
        showListening(speech = emptyList(), conversation = voiceTypedConversation())

        composeRule.onNodeWithContentDescription("Waiter: Thursday", substring = true).performClick()
        composeRule.waitForIdle()

        assertEquals(4, conversation().messages.size)
        assertEquals("Thursday", draft().text)
        composeRule.onNodeWithText("Thursday").assertIsDisplayed()
    }

    @Test
    fun tappingAVoiceMessageOnTheDeafUsersTurnDoesNothing() {
        showListening(speech = emptyList(), conversation = voiceTypedConversation().withTurn(ParticipantId.A))

        composeRule.onNodeWithContentDescription("Waiter: Thursday", substring = true).performClick()
        composeRule.waitForIdle()

        assertEquals(5, conversation().messages.size)
        assertEquals("", draft().text)
    }

    @Test
    fun tappingATypedMessageDoesNothing() {
        showListening(speech = emptyList(), conversation = voiceTypedConversation())

        composeRule.onNodeWithContentDescription("Waiter: No problem. It will take about 15 minutes.").performClick()
        composeRule.waitForIdle()

        assertEquals(5, conversation().messages.size)
    }

    @Test
    fun editIsInTheMenuAndTalkBackActions() {
        showListening(speech = emptyList(), conversation = voiceTypedConversation())

        val labels =
            composeRule
                .onNodeWithContentDescription("Waiter: Thursday", substring = true)
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .map { it.label }
        assertEquals(listOf("Edit", "Copy", "Quote", "Delete"), labels)

        longPress("Waiter: Thursday")
        listOf("Edit", "Copy", "Quote", "Delete").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Edit").performClick()
        composeRule.waitForIdle()
        assertEquals("Thursday", draft().text)
    }

    @Test
    fun editIsNotOfferedOnTypedMessages() {
        showListening(speech = emptyList(), conversation = voiceTypedConversation())

        longPress("Waiter: No problem. It will take about 15 minutes.")

        composeRule.onNodeWithText("Copy").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun editIsNotOfferedOnTheDeafUsersTurn() {
        showListening(speech = emptyList(), conversation = voiceTypedConversation().withTurn(ParticipantId.A))

        longPress("Waiter: Thursday")

        composeRule.onNodeWithText("Copy").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun notAskedExplainsThePermissionAndRequestsIt() {
        microphoneAccess = PermissionAccess.NotAsked
        showListening()

        startVoiceTyping()

        assertEquals(1, permissionRequests)
        composeRule.onNodeWithText("To use voice typing", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow microphone").assertIsDisplayed()
        composeRule.onNode(isMessageField).assertIsDisplayed()
        assertEquals(false, viewModel.voice.state.value.on)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun grantingThePermissionTurnsVoiceTypingOn() {
        microphoneAccess = PermissionAccess.NotAsked
        showListening()
        startVoiceTyping()

        microphoneAccess = PermissionAccess.Granted
        composeRule.waitForIdle()

        assertTrue(viewModel.voice.state.value.on)
        composeRule.onNodeWithText("To use voice typing", substring = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Waiter: Tuesday works").assertIsDisplayed()
    }

    @Test
    fun deniedOffersAnotherGo() {
        microphoneAccess = PermissionAccess.Denied
        showListening()
        startVoiceTyping()

        composeRule.onNodeWithText("Allow microphone").performClick()

        assertEquals(2, permissionRequests)
    }

    @Test
    fun deniedForGoodOffersSettings() {
        microphoneAccess = PermissionAccess.DeniedForGood
        showListening()

        startVoiceTyping()

        assertEquals(0, permissionRequests)
        composeRule.onNodeWithText("To use voice typing", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Allow microphone").assertDoesNotExist()
        composeRule.onNodeWithText("Open settings").assertIsDisplayed().performClick()
        assertEquals(1, settingsOpened)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun unavailableShowsTheFallbackLine() {
        show(
            orderingConversation(ParticipantId.B),
            createSpeechRecogniser = { error("The model would not load") },
            audio = HangingAudio(1),
        )

        startVoiceTyping()

        composeRule.onNodeWithText("Voice typing isn\u2019t available right now. You can still type normally.").assertIsDisplayed()
        composeRule.onNode(isMessageField).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start voice typing").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun endingWhileListeningShowsKeepDialog() {
        show(createSpeechRecogniser = { FakeSpeechRecogniser(listOf(SpeechEvent.Partial("Tuesday works"))) }, audio = HangingAudio(1))
        tapName("Them")
        composeRule.waitForIdle()
        startVoiceTyping()
        composeRule.onNodeWithContentDescription("Them: Tuesday works").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("End chat").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Keep this chat?").assertIsDisplayed()
        assertEquals(0, ended)
        assertEquals("Tuesday works", conversation().messages.last().plainText)
    }
}
