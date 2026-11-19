package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.FormattedText
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange
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
class ChatViewModelEditTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val store = FakeSavedChatStore()

    private fun TestScope.viewModel(
        conversation: Conversation = orderingConversation(ParticipantId.A),
        savedState: SavedStateHandle = SavedStateHandle(mapOf("conversation" to ChatCodec.encode(conversation))),
        speech: List<SpeechEvent?> = emptyList(),
    ): ChatViewModel {
        val viewModel =
            ChatViewModel(
                FakeSettingsRepository(),
                savedState,
                ChatOpening(),
                ChatInsertions(),
                store,
                CoroutineScope(UnconfinedTestDispatcher()),
                { FakeSpeechRecogniser(speech) },
                HangingAudio(1),
                StandardTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    private fun ChatViewModel.type(text: String) = drafts.setComposition(Composition().withText(text, text.length, text.length))

    private fun ChatViewModel.draft(): Draft = assertIs<ChatUiState.Ready>(state.value).draft

    private fun ChatViewModel.conversation(): Conversation = assertIs<ChatUiState.Ready>(state.value).conversation

    @Test
    fun editingLoadsTextFormattingAndQuoteAndSetsDraftAside() =
        runTest {
            val viewModel = viewModel()
            viewModel.quote(0)
            viewModel.type("Half typed")

            viewModel.startEdit(2)

            val draft = viewModel.draft()
            val message = viewModel.conversation().messages[2]
            assertEquals(message.plainText, draft.text)
            assertEquals(FormattedText.of(message.spans), draft.composition.content)
            assertTrue(
                draft.composition.content.ranges
                    .any { it.style == SpanStyle.Bold },
            )
            assertEquals(message.plainText.length, draft.composition.selectionStart)
            assertEquals(message.quote, draft.quote)
            assertEquals(2, draft.editing?.messageId)
            assertEquals("Half typed", draft.editing?.resume?.text)
            assertEquals(Quote(0, "Hi! I'm deaf. Could I have pasta arrabbiata please?"), draft.editing?.resume?.quote)
            assertEquals(4, viewModel.conversation().messages.size)
        }

    @Test
    fun sendingAnEditReplacesInPlaceAndRestoresTheDraft() =
        runTest {
            val viewModel = viewModel()
            viewModel.quote(0)
            viewModel.type("Half typed")
            val before = viewModel.conversation()
            viewModel.startEdit(2)

            viewModel.drafts.setComposition(
                Composition(FormattedText("Medium please", listOf(StyleRange(0, 6, SpanStyle.Italic)))),
            )
            viewModel.send()

            val conversation = viewModel.conversation()
            assertEquals(4, conversation.messages.size)
            val message = conversation.messages[2]
            assertEquals("Medium please", message.plainText)
            assertEquals(ParticipantId.A, message.author)
            assertTrue(message.edited)
            assertEquals(before.messages[2].quote, message.quote)
            assertEquals(before.messages[1], conversation.messages[1])
            assertEquals(before.messages[3], conversation.messages[3])
            val draft = viewModel.draft()
            assertNull(draft.editing)
            assertEquals("Half typed", draft.text)
            assertEquals(Quote(0, "Hi! I'm deaf. Could I have pasta arrabbiata please?"), draft.quote)
        }

    @Test
    fun sendingAnEditWithABlankFieldDoesNothing() =
        runTest {
            val viewModel = viewModel()
            viewModel.startEdit(2)
            viewModel.type("   ")

            viewModel.send()

            assertEquals(orderingConversation(ParticipantId.A), viewModel.conversation())
            assertEquals(2, viewModel.draft().editing?.messageId)
        }

    @Test
    fun cancellingRestoresTheDraftAndLeavesTheMessage() =
        runTest {
            val viewModel = viewModel()
            val before = viewModel.conversation()
            viewModel.type("Half typed")
            viewModel.startEdit(2)
            viewModel.type("Something else")

            viewModel.drafts.cancelEdit()

            assertEquals(before, viewModel.conversation())
            assertEquals(Draft(Composition().withText("Half typed", 10, 10)), viewModel.draft())
        }

    @Test
    fun onlyOwnMessagesOnOwnTurnCanBeEdited() =
        runTest {
            val viewModel = viewModel(orderingConversation(ParticipantId.A))

            viewModel.startEdit(1)
            assertNull(viewModel.draft().editing)
            viewModel.startEdit(2)
            assertEquals(2, viewModel.draft().editing?.messageId)
            viewModel.drafts.cancelEdit()

            viewModel.selectTurn(ParticipantId.B)
            viewModel.startEdit(2)
            assertNull(viewModel.draft().editing)
            viewModel.startEdit(3)
            assertEquals(3, viewModel.draft().editing?.messageId)
            viewModel.drafts.cancelEdit()

            viewModel.startEdit(0)
            assertNull(viewModel.draft().editing)
            viewModel.startEdit(9)
            assertNull(viewModel.draft().editing)
        }

    @Test
    fun editingWorksOnVoiceTypedMessages() =
        runTest {
            val viewModel = viewModel(orderingConversation(ParticipantId.B).appendVoiceTyped("Thursday"))

            viewModel.startEdit(4)
            viewModel.type("Friday")
            viewModel.send()

            val message = viewModel.conversation().messages[4]
            assertEquals("Friday", message.plainText)
            assertTrue(message.voiceTyped)
            assertTrue(message.edited)
        }

    @Test
    fun startingAnEditTurnsVoiceTypingOff() =
        runTest {
            val viewModel = viewModel(orderingConversation(ParticipantId.B).appendVoiceTyped("Thursday"))
            viewModel.setListeningAllowed(foreground = true, permitted = true)
            viewModel.voice.turnOn()
            runCurrent()
            assertTrue(viewModel.voice.state.value.on)

            viewModel.startEdit(4)
            runCurrent()

            assertFalse(viewModel.voice.state.value.on)
            assertEquals(4, viewModel.draft().editing?.messageId)
        }

    @Test
    fun deletingTheEditedMessageCancelsTheEdit() =
        runTest {
            val viewModel = viewModel()
            viewModel.type("Half typed")
            viewModel.startEdit(2)

            viewModel.deleteMessage(2)

            assertEquals(3, viewModel.conversation().messages.size)
            val draft = viewModel.draft()
            assertNull(draft.editing)
            assertEquals("Half typed", draft.text)
        }

    @Test
    fun deletingAnEarlierMessageKeepsEditingTheRightOne() =
        runTest {
            val viewModel = viewModel()
            viewModel.quote(1)
            viewModel.startEdit(2)

            viewModel.deleteMessage(0)

            val draft = viewModel.draft()
            assertEquals(1, draft.editing?.messageId)
            assertEquals(
                0,
                draft.editing
                    ?.resume
                    ?.quote
                    ?.messageId,
            )
            viewModel.type("Medium please")
            viewModel.send()
            assertEquals(listOf(false, true, false), viewModel.conversation().messages.map { it.edited })
            assertEquals("Medium please", viewModel.conversation().messages[1].plainText)
        }

    @Test
    fun deletingTheQuotedMessageWhileEditingKeepsTheQuoteTextUnlinked() =
        runTest {
            val viewModel = viewModel()
            viewModel.startEdit(2)

            viewModel.deleteMessage(1)

            assertEquals(Quote(null, "spicy or mild?"), viewModel.draft().quote)
            assertEquals(1, viewModel.draft().editing?.messageId)
        }

    @Test
    fun aMessageCannotQuoteItselfWhileBeingEdited() =
        runTest {
            val viewModel = viewModel()
            viewModel.startEdit(2)

            viewModel.quote(2)

            assertEquals(viewModel.conversation().messages[2].quote, viewModel.draft().quote)
        }

    @Test
    fun switchingTurnsMidEditKeepsTheEditWithItsParticipant() =
        runTest {
            val viewModel = viewModel()
            viewModel.startEdit(2)

            viewModel.selectTurn(ParticipantId.B)
            assertNull(viewModel.draft().editing)
            viewModel.selectTurn(ParticipantId.A)

            assertEquals(2, viewModel.draft().editing?.messageId)
        }

    @Test
    fun editingSurvivesProcessDeath() =
        runTest {
            val savedState = SavedStateHandle(mapOf("conversation" to ChatCodec.encode(orderingConversation(ParticipantId.A))))
            val first = viewModel(savedState = savedState)
            first.type("Half typed")
            first.startEdit(2)
            first.type("Medium please")

            val second = viewModel(savedState = savedState)

            val draft = second.draft()
            assertEquals(2, draft.editing?.messageId)
            assertEquals("Medium please", draft.text)
            assertEquals("Half typed", draft.editing?.resume?.text)
            second.send()
            assertEquals("Medium please", second.conversation().messages[2].plainText)
            assertEquals("Half typed", second.draft().text)
        }
}
