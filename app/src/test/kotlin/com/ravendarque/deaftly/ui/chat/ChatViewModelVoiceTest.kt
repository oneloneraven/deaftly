package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.speech.FakeSpeechRecogniser
import com.ravendarque.deaftly.core.speech.SpeechEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelVoiceTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val store = FakeSavedChatStore()

    private fun TestScope.listening(
        speech: List<SpeechEvent?>,
        audio: HangingAudio,
        firstMessage: String? = null,
    ): ChatViewModel {
        val viewModel =
            ChatViewModel(
                FakeSettingsRepository(),
                SavedStateHandle(),
                ChatOpening(listOfNotNull(firstMessage)),
                ChatInsertions(),
                store,
                CoroutineScope(UnconfinedTestDispatcher()),
                { FakeSpeechRecogniser(speech) },
                audio,
                StandardTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.selectTurn(ParticipantId.B)
        viewModel.setListeningAllowed(foreground = true, permitted = true)
        viewModel.voice.turnOn()
        runCurrent()
        return viewModel
    }

    private fun ChatViewModel.type(text: String) = drafts.setComposition(Composition().withText(text, text.length, text.length))

    private fun ChatViewModel.draft(): Draft = assertIs<ChatUiState.Ready>(state.value).draft

    private fun ChatViewModel.conversation(): Conversation = assertIs<ChatUiState.Ready>(state.value).conversation

    @Test
    fun utterancesAreAppendedAsVoiceTypedFromB() =
        runTest {
            val viewModel = listening(listOf(SpeechEvent.Utterance("Tuesday works")), HangingAudio(1))

            val message = viewModel.conversation().messages.single()
            assertEquals(ParticipantId.B, message.author)
            assertEquals("Tuesday works", message.plainText)
            assertTrue(message.voiceTyped)
        }

    @Test
    fun turningToAStopsListening() =
        runTest {
            val audio = HangingAudio(1)
            val viewModel = listening(listOf(SpeechEvent.Partial("Half a")), audio)
            assertTrue(audio.isOpen)

            viewModel.selectTurn(ParticipantId.A)
            runCurrent()

            assertFalse(audio.isOpen)
            val message = viewModel.conversation().messages.single()
            assertEquals(ParticipantId.B, message.author)
            assertEquals("Half a", message.plainText)
            assertTrue(viewModel.voice.state.value.on)
        }

    @Test
    fun returningToBListensAgain() =
        runTest {
            val audio = HangingAudio(0)
            val viewModel = listening(emptyList(), audio)
            viewModel.selectTurn(ParticipantId.A)
            runCurrent()
            assertEquals(1, audio.opened)

            viewModel.selectTurn(ParticipantId.B)
            runCurrent()

            assertEquals(2, audio.opened)
            assertTrue(audio.isOpen)
        }

    @Test
    fun notListeningInTheBackgroundOrWithoutPermission() =
        runTest {
            val audio = HangingAudio(0)
            val viewModel = listening(emptyList(), audio)

            viewModel.setListeningAllowed(foreground = false, permitted = true)
            runCurrent()
            assertFalse(audio.isOpen)

            viewModel.setListeningAllowed(foreground = true, permitted = false)
            runCurrent()
            assertFalse(audio.isOpen)
            assertEquals(1, audio.opened)
        }

    @Test
    fun editAppendsToDraftAndUnlinksQuotes() =
        runTest {
            val viewModel = listening(listOf(SpeechEvent.Utterance("Thursday")), HangingAudio(1), firstMessage = "Hello")
            viewModel.quote(1)
            viewModel.type("Is that fine?")
            viewModel.send()
            viewModel.type("Sorry")

            viewModel.editVoiceMessage(1)
            runCurrent()

            val conversation = viewModel.conversation()
            assertFalse(viewModel.voice.state.value.on)
            assertEquals(listOf("Hello", "Is that fine?"), conversation.messages.map { it.plainText })
            val quote = conversation.messages.last().quote
            assertNull(quote?.messageId)
            assertEquals("Thursday", quote?.text)
            assertEquals("Sorry Thursday", viewModel.draft().text)
        }

    @Test
    fun editDoesNothingOnTheDeafUsersTurn() =
        runTest {
            val viewModel = listening(listOf(SpeechEvent.Utterance("Thursday")), HangingAudio(1))
            viewModel.selectTurn(ParticipantId.A)
            runCurrent()

            viewModel.editVoiceMessage(0)

            assertEquals(listOf("Thursday"), viewModel.conversation().messages.map { it.plainText })
            assertTrue(viewModel.voice.state.value.on)
            assertEquals("", viewModel.draft().text)
        }

    @Test
    fun editIgnoresTypedMessages() =
        runTest {
            val viewModel = listening(emptyList(), HangingAudio(0))
            viewModel.type("Typed")
            viewModel.send()

            viewModel.editVoiceMessage(0)

            assertEquals(listOf("Typed"), viewModel.conversation().messages.map { it.plainText })
            assertTrue(viewModel.voice.state.value.on)
            assertEquals("", viewModel.draft().text)
        }
}
