package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.FormattedText
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange
import com.ravendarque.deaftly.core.settings.ChatTextSize
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
        firstMessage: String? = null,
        draft: String = "",
        insertions: ChatInsertions = ChatInsertions(),
    ) = ChatViewModel(settings, savedState, ChatOpening(listOfNotNull(firstMessage), draft), insertions)

    private fun ChatViewModel.type(text: String) = drafts.setComposition(Composition().withText(text, text.length, text.length))

    private fun ChatViewModel.draft(): Draft = assertIs<ChatUiState.Ready>(state.value).draft

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
            val viewModel = viewModel(FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green)))
            collect(viewModel)

            assertEquals(Participant(ParticipantId.A, ColourRole.Green, "Alex"), viewModel.conversation().participantA)
            assertNull(viewModel.conversation().participantB.name)
        }

    @Test
    fun updatingAParticipantChangesOnlyThatPerson() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.updateParticipant(Participant(ParticipantId.B, ColourRole.Blue, "Sam"))

            val conversation = viewModel.conversation()
            assertEquals(Participant(ParticipantId.B, ColourRole.Blue, "Sam"), conversation.participantB)
            assertEquals(Participant.defaultA(), conversation.participantA)
        }

    @Test
    fun aColourAlreadyTakenByTheOtherPersonIsRejected() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            assertFailsWith<IllegalArgumentException> {
                viewModel.updateParticipant(Participant(ParticipantId.A, ColourRole.Orange))
            }
        }

    @Test
    fun editingDoesNotChangeTheRememberedDefaultsUntilAskedTo() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)
            collect(viewModel)

            viewModel.updateParticipant(Participant(ParticipantId.A, ColourRole.Red, "Alex"))

            assertEquals(ChatProfile(), settings.saved)
        }

    @Test
    fun rememberingSavesOnlyTheDeafUsersNameAndColour() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)
            collect(viewModel)
            viewModel.updateParticipant(Participant(ParticipantId.A, ColourRole.Red, "Alex"))
            viewModel.updateParticipant(Participant(ParticipantId.B, ColourRole.Blue, "Sam"))

            viewModel.rememberOwnDefaults()

            assertEquals(ChatProfile("Alex", ColourRole.Red), settings.saved)
        }

    @Test
    fun theOtherPersonStartsFreshEachChat() =
        runTest {
            val settings = FakeSettingsRepository()
            val first = viewModel(settings)
            collect(first)
            first.updateParticipant(Participant(ParticipantId.B, ColourRole.Blue, "Sam"))
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
            first.updateParticipant(Participant(ParticipantId.A, ColourRole.Red, "Alex"))
            first.updateParticipant(Participant(ParticipantId.B, ColourRole.Blue, "Sam"))

            val restored = viewModel(FakeSettingsRepository(ChatProfile("Other", ColourRole.Green)), savedState)
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

            viewModel.type("  Hello there  ")
            viewModel.send()

            val conversation = viewModel.conversation()
            assertEquals(listOf("Hello there"), conversation.messages.map { it.plainText })
            assertEquals(ParticipantId.A, conversation.messages.single().author)
            assertEquals(ParticipantId.A, conversation.turn)
            assertEquals("", assertIs<ChatUiState.Ready>(viewModel.state.value).draft.text)
        }

    @Test
    fun aBlankDraftIsNotSent() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.type("   ")
            viewModel.send()

            assertEquals(0, viewModel.conversation().messages.size)
        }

    @Test
    fun selectingTheTurnChangesTheAuthorOfTheNextMessage() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.selectTurn(ParticipantId.B)
            viewModel.type("Hi")
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
            viewModel.type("from me")

            viewModel.selectTurn(ParticipantId.B)
            assertEquals("", assertIs<ChatUiState.Ready>(viewModel.state.value).draft.text)
            viewModel.type("from them")
            viewModel.selectTurn(ParticipantId.A)

            assertEquals("from me", assertIs<ChatUiState.Ready>(viewModel.state.value).draft.text)
        }

    @Test
    fun theTextSizeStartsMediumAndCyclesThroughThreeSizesAndBack() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            fun size() = assertIs<ChatUiState.Ready>(viewModel.state.value).textSize

            assertEquals(ChatTextSize.Medium, size())
            viewModel.cycleTextSize()
            assertEquals(ChatTextSize.Large, size())
            viewModel.cycleTextSize()
            assertEquals(ChatTextSize.Small, size())
            viewModel.cycleTextSize()
            assertEquals(ChatTextSize.Medium, size())
        }

    @Test
    fun theTextSizeAndSidesAreSavedInSettingsAndRememberedByTheNextChat() =
        runTest {
            val settings = FakeSettingsRepository()
            val first = viewModel(settings)
            collect(first)
            first.cycleTextSize()
            first.switchSides()

            val next = viewModel(settings)
            collect(next)

            val state = assertIs<ChatUiState.Ready>(next.state.value)
            assertEquals(ChatTextSize.Large, state.textSize)
            assertEquals(true, state.sidesSwapped)
        }

    @Test
    fun sidesStartAsTheyWereAndSwitchBackAndForth() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            fun swapped() = assertIs<ChatUiState.Ready>(viewModel.state.value).sidesSwapped

            assertEquals(false, swapped())
            viewModel.switchSides()
            assertEquals(true, swapped())
            viewModel.switchSides()
            assertEquals(false, swapped())
        }

    @Test
    fun switchingSidesChangesNeitherTheTurnNorTheMessages() =
        runTest {
            val viewModel = viewModel(firstMessage = "Hello")
            collect(viewModel)
            val before = viewModel.conversation()

            viewModel.switchSides()

            assertEquals(before, viewModel.conversation())
        }

    @Test
    fun messagesTurnAndDraftsSurviveRecreationFromSavedState() =
        runTest {
            val settings = FakeSettingsRepository()
            val savedState = SavedStateHandle()
            val first = viewModel(settings, savedState)
            collect(first)
            first.type("one")
            first.send()
            first.selectTurn(ParticipantId.B)
            first.type("typing")
            first.cycleTextSize()

            val restored = viewModel(settings, savedState)
            collect(restored)

            assertEquals(first.state.value, restored.state.value)
            assertEquals("typing", assertIs<ChatUiState.Ready>(restored.state.value).draft.text)
        }

    @Test
    fun aNewViewModelStartsAnEmptyConversation() =
        runTest {
            val settings = FakeSettingsRepository()
            val first = viewModel(settings)
            collect(first)
            first.type("gone soon")
            first.send()

            val second = viewModel(settings)
            collect(second)

            assertEquals(0, second.conversation().messages.size)
        }

    @Test
    fun aFirstMessageStartsTheChatAsTheDeafUsersTurnAndPassesTheTurnToTheOtherPerson() =
        runTest {
            val viewModel = viewModel(firstMessage = "Table for two, please")
            collect(viewModel)

            val conversation = viewModel.conversation()
            val message = conversation.messages.single()
            assertEquals("Table for two, please", message.plainText)
            assertEquals(ParticipantId.A, message.author)
            assertEquals(ParticipantId.B, conversation.turn)
        }

    @Test
    fun theFirstMessageUsesTheRememberedName() =
        runTest {
            val viewModel = viewModel(FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green)), firstMessage = "Hello")
            collect(viewModel)

            assertEquals("Alex", viewModel.conversation().participantA.name)
        }

    @Test
    fun aBlankFirstMessageStartsAnEmptyChat() =
        runTest {
            val viewModel = viewModel(firstMessage = "   ")
            collect(viewModel)

            assertEquals(0, viewModel.conversation().messages.size)
            assertEquals(ParticipantId.A, viewModel.conversation().turn)
        }

    @Test
    fun recreatingTheChatDoesNotAddTheFirstMessageAgain() =
        runTest {
            val savedState = SavedStateHandle()
            collect(viewModel(savedState = savedState, firstMessage = "Hello"))

            val restored = viewModel(savedState = savedState, firstMessage = "Hello")
            collect(restored)

            assertEquals(1, restored.conversation().messages.size)
        }

    @Test
    fun aSentMessageKeepsTheDraftsFormatting() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.drafts.setComposition(
                Composition(FormattedText("  Mild, no nuts  ", listOf(StyleRange(8, 15, SpanStyle.Bold)))),
            )

            viewModel.send()

            val message = viewModel.conversation().messages.single()
            assertEquals(listOf(Span("Mild, "), Span("no nuts", setOf(SpanStyle.Bold))), message.spans)
        }

    @Test
    fun theFormattingOfADraftSurvivesRecreationFromSavedState() =
        runTest {
            val savedState = SavedStateHandle()
            val first = viewModel(savedState = savedState)
            collect(first)
            first.drafts.setComposition(
                Composition(FormattedText("Hello", listOf(StyleRange(0, 5, SpanStyle.Italic))), 1, 3, setOf(SpanStyle.Bold)),
            )

            val restored = viewModel(savedState = savedState)
            collect(restored)

            assertEquals(first.draft(), restored.draft())
        }

    @Test
    fun aGarbledSavedDraftBecomesAnEmptyOne() =
        runTest {
            val savedState = SavedStateHandle(mapOf("draft_a" to "{\"text\":\"ab\",\"selectionStart\":9}"))
            val viewModel = viewModel(savedState = savedState)
            collect(viewModel)

            assertEquals(Draft(), viewModel.draft())
        }

    @Test
    fun quotingAMessageWaitsForTheNextMessageAndIsThenCleared() =
        runTest {
            val viewModel = viewModel(firstMessage = "Table for two, please")
            collect(viewModel)

            viewModel.quote(0, "for two")
            assertEquals(Quote(0, "for two"), viewModel.draft().quote)
            viewModel.type("Certainly")
            viewModel.send()

            assertEquals(
                Quote(0, "for two"),
                viewModel
                    .conversation()
                    .messages
                    .last()
                    .quote,
            )
            assertNull(viewModel.draft().quote)
        }

    @Test
    fun quotingWithoutAnExcerptQuotesTheWholeMessage() =
        runTest {
            val viewModel = viewModel(firstMessage = "Table for two, please")
            collect(viewModel)

            viewModel.quote(0)

            assertEquals(Quote(0, "Table for two, please"), viewModel.draft().quote)
        }

    @Test
    fun aQuoteOfAnUnknownMessageIsIgnored() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.quote(4)

            assertNull(viewModel.draft().quote)
        }

    @Test
    fun theQuoteBelongsToThePersonWhoChoseIt() =
        runTest {
            val viewModel = viewModel(firstMessage = "Table for two, please")
            collect(viewModel)
            viewModel.quote(0)

            viewModel.selectTurn(ParticipantId.A)

            assertNull(viewModel.draft().quote)
        }

    @Test
    fun quotingTheLatestPicksTheOtherPersonsLastMessage() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.type("first")
            viewModel.send()
            viewModel.selectTurn(ParticipantId.B)
            viewModel.type("second")
            viewModel.send()
            viewModel.type("third")
            viewModel.send()
            viewModel.selectTurn(ParticipantId.A)

            viewModel.quoteLatest()

            assertEquals(Quote(2, "third"), viewModel.draft().quote)
        }

    @Test
    fun removingTheQuoteDropsIt() =
        runTest {
            val viewModel = viewModel(firstMessage = "Hello")
            collect(viewModel)
            viewModel.quote(0)

            viewModel.drafts.setQuote(null)

            assertNull(viewModel.draft().quote)
        }

    @Test
    fun aQuoteAloneIsNotEnoughToSend() =
        runTest {
            val viewModel = viewModel(firstMessage = "Hello")
            collect(viewModel)
            viewModel.quote(0)

            viewModel.send()

            assertEquals(1, viewModel.conversation().messages.size)
        }

    @Test
    fun insertedTextReplacesTheSelectionInTheActiveDraft() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.drafts.setComposition(Composition(FormattedText("Hello world")).select(6, 11))

            viewModel.drafts.insertText("there")

            assertEquals("Hello there", viewModel.draft().text)
        }

    @Test
    fun anOfferedInsertionLandsAtTheCursorOfTheActiveDraftAndIsConsumed() =
        runTest {
            val insertions = ChatInsertions()
            val viewModel = viewModel(insertions = insertions)
            collect(viewModel)
            viewModel.drafts.setComposition(Composition(FormattedText("Hello world")).select(5, 5))

            insertions.offer(", dear")

            assertEquals("Hello, dear world", viewModel.draft().text)
            assertEquals(11, viewModel.draft().composition.selectionStart)
            assertNull(insertions.text.value)
        }

    @Test
    fun anInsertionIsNotSentOnItsOwn() =
        runTest {
            val insertions = ChatInsertions()
            val viewModel = viewModel(insertions = insertions)
            collect(viewModel)

            insertions.offer("Table for two")

            assertEquals(0, viewModel.conversation().messages.size)
            assertEquals("Table for two", viewModel.draft().text)
        }

    @Test
    fun anOpeningWithSeveralMessagesMakesEachOneFromTheDeafUserInOrderAndPassesTheTurn() =
        runTest {
            val viewModel =
                ChatViewModel(
                    FakeSettingsRepository(),
                    SavedStateHandle(),
                    ChatOpening(listOf("Hello, I am deaf", "  ", "Could I have a coffee")),
                    ChatInsertions(),
                )
            collect(viewModel)

            val conversation = viewModel.conversation()
            assertEquals(listOf("Hello, I am deaf", "Could I have a coffee"), conversation.messages.map { it.plainText })
            assertEquals(listOf(ParticipantId.A, ParticipantId.A), conversation.messages.map { it.author })
            assertEquals(ParticipantId.B, conversation.turn)
        }

    @Test
    fun anOpeningDraftGoesIntoTheDeafUsersComposer() =
        runTest {
            val viewModel = viewModel(firstMessage = "Hello", draft = "And a tea")
            collect(viewModel)

            val drafts = assertIs<ChatUiState.Ready>(viewModel.state.value).drafts
            assertEquals("And a tea", drafts.getValue(ParticipantId.A).composition.text)
            assertEquals("", drafts.getValue(ParticipantId.B).composition.text)
        }

    @Test
    fun anOpeningDraftWithNoMessagesLeavesTheTurnWithTheDeafUser() =
        runTest {
            val viewModel = viewModel(draft = "Half a thought")
            collect(viewModel)

            assertEquals(ParticipantId.A, viewModel.conversation().turn)
            assertEquals("Half a thought", viewModel.draft().text)
        }
}
