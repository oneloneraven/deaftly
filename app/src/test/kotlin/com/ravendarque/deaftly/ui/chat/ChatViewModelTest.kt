package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = ChatViewModel(settings, savedState)

    private fun ChatViewModel.conversation(): Conversation = assertIs<ChatUiState.Ready>(state.value).conversation

    private fun TestScope.collect(viewModel: ChatViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

    @Test
    fun startsWithTheDefaultColoursAndNoNames() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            val conversation = viewModel.conversation()
            assertEquals(Participant.defaultA(), conversation.participantA)
            assertEquals(Participant.defaultB(), conversation.participantB)
        }

    @Test
    fun usesTheRememberedNameAndColourForTheDeafUser() =
        runTest {
            val viewModel = viewModel(FakeSettingsRepository(ChatProfile("Alex", ColourRole.Teal)))
            collect(viewModel)

            assertEquals(Participant(ParticipantId.A, ColourRole.Teal, "Alex"), viewModel.conversation().participantA)
            assertNull(viewModel.conversation().participantB.name)
        }

    @Test
    fun updatingAParticipantChangesOnlyThatPerson() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.updateParticipant(Participant(ParticipantId.B, ColourRole.Berry, "Sam"))

            val conversation = viewModel.conversation()
            assertEquals(Participant(ParticipantId.B, ColourRole.Berry, "Sam"), conversation.participantB)
            assertEquals(Participant.defaultA(), conversation.participantA)
        }

    @Test
    fun aColourAlreadyTakenByTheOtherPersonIsRejected() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            assertFailsWith<IllegalArgumentException> {
                viewModel.updateParticipant(Participant(ParticipantId.A, ColourRole.PersonB))
            }
        }

    @Test
    fun editingDoesNotChangeTheRememberedDefaultsUntilAskedTo() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)
            collect(viewModel)

            viewModel.updateParticipant(Participant(ParticipantId.A, ColourRole.Rose, "Alex"))

            assertEquals(ChatProfile(), settings.saved)
        }

    @Test
    fun rememberingSavesOnlyTheDeafUsersNameAndColour() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)
            collect(viewModel)
            viewModel.updateParticipant(Participant(ParticipantId.A, ColourRole.Rose, "Alex"))
            viewModel.updateParticipant(Participant(ParticipantId.B, ColourRole.Berry, "Sam"))

            viewModel.rememberOwnDefaults()

            assertEquals(ChatProfile("Alex", ColourRole.Rose), settings.saved)
        }

    @Test
    fun theOtherPersonStartsFreshEachChat() =
        runTest {
            val settings = FakeSettingsRepository()
            val first = viewModel(settings)
            collect(first)
            first.updateParticipant(Participant(ParticipantId.B, ColourRole.Berry, "Sam"))
            first.rememberOwnDefaults()

            val second = viewModel(settings)
            collect(second)

            assertEquals(Participant.defaultB(), second.conversation().participantB)
        }

    @Test
    fun theParticipantsSurviveRecreationFromSavedState() =
        runTest {
            val savedState = SavedStateHandle()
            val first = viewModel(savedState = savedState)
            collect(first)
            first.updateParticipant(Participant(ParticipantId.A, ColourRole.Rose, "Alex"))
            first.updateParticipant(Participant(ParticipantId.B, ColourRole.Berry, "Sam"))

            val restored = viewModel(FakeSettingsRepository(ChatProfile("Other", ColourRole.Teal)), savedState)
            collect(restored)

            assertEquals(first.conversation(), restored.conversation())
        }

    @Test
    fun unreadableSavedStateFallsBackToTheDefaults() =
        runTest {
            val savedState = SavedStateHandle(mapOf("conversation" to "not json"))
            val viewModel = viewModel(savedState = savedState)
            collect(viewModel)

            assertEquals(Participant.defaultA(), viewModel.conversation().participantA)
        }

    @Test
    fun sendingAppendsTheDraftAsTheActivePersonWithoutSwitchingTurn() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.setDraft("  Hello there  ")
            viewModel.send()

            val conversation = viewModel.conversation()
            assertEquals(listOf("Hello there"), conversation.messages.map { it.plainText })
            assertEquals(ParticipantId.A, conversation.messages.single().author)
            assertEquals(ParticipantId.A, conversation.turn)
            assertEquals("", assertIs<ChatUiState.Ready>(viewModel.state.value).draft)
        }

    @Test
    fun aBlankDraftIsNotSent() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.setDraft("   ")
            viewModel.send()

            assertEquals(0, viewModel.conversation().messages.size)
        }

    @Test
    fun selectingTheTurnChangesTheAuthorOfTheNextMessage() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.selectTurn(ParticipantId.B)
            viewModel.setDraft("Hi")
            viewModel.send()

            assertEquals(
                ParticipantId.B,
                viewModel
                    .conversation()
                    .messages
                    .single()
                    .author,
            )
        }

    @Test
    fun eachPersonKeepsTheirOwnDraftWhenTheTurnSwitches() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.setDraft("from me")

            viewModel.selectTurn(ParticipantId.B)
            assertEquals("", assertIs<ChatUiState.Ready>(viewModel.state.value).draft)
            viewModel.setDraft("from them")
            viewModel.selectTurn(ParticipantId.A)

            assertEquals("from me", assertIs<ChatUiState.Ready>(viewModel.state.value).draft)
        }

    @Test
    fun theTextSizeCyclesThroughThreeSizesAndBack() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            fun size() = assertIs<ChatUiState.Ready>(viewModel.state.value).textSize

            assertEquals(ChatTextSize.Normal, size())
            viewModel.cycleTextSize()
            assertEquals(ChatTextSize.Large, size())
            viewModel.cycleTextSize()
            assertEquals(ChatTextSize.Largest, size())
            viewModel.cycleTextSize()
            assertEquals(ChatTextSize.Normal, size())
        }

    @Test
    fun messagesTurnTextSizeAndDraftsSurviveRecreationFromSavedState() =
        runTest {
            val savedState = SavedStateHandle()
            val first = viewModel(savedState = savedState)
            collect(first)
            first.setDraft("one")
            first.send()
            first.selectTurn(ParticipantId.B)
            first.setDraft("typing")
            first.cycleTextSize()

            val restored = viewModel(FakeSettingsRepository(ChatProfile("Other", ColourRole.Teal)), savedState)
            collect(restored)

            assertEquals(first.state.value, restored.state.value)
            assertEquals("typing", assertIs<ChatUiState.Ready>(restored.state.value).draft)
        }

    @Test
    fun aNewViewModelStartsAnEmptyConversation() =
        runTest {
            val settings = FakeSettingsRepository()
            val first = viewModel(settings)
            collect(first)
            first.setDraft("gone soon")
            first.send()

            val second = viewModel(settings)
            collect(second)

            assertEquals(0, second.conversation().messages.size)
        }
}
