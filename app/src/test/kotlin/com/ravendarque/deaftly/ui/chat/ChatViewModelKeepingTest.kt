package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelKeepingTest {
    private val store = FakeSavedChatStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(savedChatId: String? = null) =
        ChatViewModel(
            FakeSettingsRepository(),
            SavedStateHandle(),
            ChatOpening(savedChatId = savedChatId),
            ChatInsertions(),
            store,
            CoroutineScope(UnconfinedTestDispatcher()),
            ::silentRecogniser,
            silentAudio(),
        )

    private fun ChatViewModel.type(text: String) = drafts.setComposition(Composition().withText(text, text.length, text.length))

    private fun ChatViewModel.conversation(): Conversation = assertIs<ChatUiState.Ready>(state.value).conversation

    private fun ChatViewModel.kept(): Boolean = assertIs<ChatUiState.Ready>(state.value).kept

    private suspend fun ChatViewModel.stored(): Conversation? = keeper.keptId.value?.let { store.load(it)?.conversation }

    private fun TestScope.collect(viewModel: ChatViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

    @Test
    fun everyChangeToAKeptChatIsSaved() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.type("Hello")
            viewModel.send()
            viewModel.keeper.keep(viewModel.conversation())

            viewModel.type("Second")
            viewModel.send()
            assertEquals(viewModel.conversation(), viewModel.stored())
            viewModel.deleteMessage(0)
            assertEquals(viewModel.conversation(), viewModel.stored())
            viewModel.updateParticipant(Participant(ParticipantId.B, ColourRole.Blue, "Kwame"))
            assertEquals(viewModel.conversation(), viewModel.stored())
            viewModel.selectTurn(ParticipantId.A)
            assertEquals(viewModel.conversation(), viewModel.stored())
            assertTrue(viewModel.kept())
        }

    @Test
    fun anUnkeptChatIsNeverSaved() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.type("Hello")
            viewModel.send()
            viewModel.type("Again")
            viewModel.send()

            assertEquals(0, store.saves)
            assertFalse(viewModel.kept())
        }

    @Test
    fun reopeningRestoresTheConversationAndIsKept() =
        runTest {
            val id = store.newId()
            val saved =
                Conversation(Participant(ParticipantId.A, ColourRole.Red, "Amira"), Participant(ParticipantId.B, ColourRole.Blue, "Kwame"))
                    .append(RichText.spans("See you at 6"))
                    .withTurn(ParticipantId.B)
            store.save(id, saved)

            val viewModel = viewModel(savedChatId = id)
            collect(viewModel)

            assertEquals(saved, viewModel.conversation())
            assertTrue(viewModel.kept())
        }

    @Test
    fun reopeningAMissingChatStartsAFreshUnkeptOne() =
        runTest {
            val viewModel = viewModel(savedChatId = store.newId())
            collect(viewModel)

            assertEquals(Conversation(Participant.defaultA(), Participant.defaultB()), viewModel.conversation())
            assertFalse(viewModel.kept())
        }
}
