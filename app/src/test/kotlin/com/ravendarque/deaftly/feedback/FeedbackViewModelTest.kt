package com.ravendarque.deaftly.feedback

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {
    private val sent = mutableListOf<FeedbackSubmission>()
    private var result = SendResult.Sent
    private val sender =
        FeedbackSender { submission ->
            sent += submission
            result
        }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        kind: FeedbackKind = FeedbackKind.Issue,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = FeedbackViewModel(kind, sender, savedState)

    @Test
    fun anEmptyMessageIsNotSent() {
        val viewModel = viewModel()
        viewModel.setMessage("   ")

        viewModel.send()

        assertEquals(setOf(FeedbackProblem.MessageEmpty), viewModel.state.value.problems)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun aMessageOverTheLimitIsNotSent() {
        val viewModel = viewModel()
        viewModel.setMessage("a".repeat(MESSAGE_MAX + 1))

        viewModel.send()

        assertEquals(setOf(FeedbackProblem.MessageTooLong), viewModel.state.value.problems)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun anEmailThatDoesNotLookLikeOneIsNotSentAndEditingClearsTheProblem() {
        val viewModel = viewModel()
        viewModel.setMessage("Hello")
        viewModel.setEmail("sam at example")

        viewModel.send()
        assertEquals(setOf(FeedbackProblem.EmailInvalid), viewModel.state.value.problems)

        viewModel.setEmail("sam@example.com")
        assertTrue(
            viewModel.state.value.problems
                .isEmpty(),
        )
    }

    @Test
    fun sendingSendsWhatWasTypedAndShowsThanks() {
        val viewModel = viewModel(FeedbackKind.Feedback)
        viewModel.setMessage("Love it")
        viewModel.setEmail("sam@example.com")
        viewModel.setQuoteConsent(true)
        viewModel.setCredit(Credit.Name)
        viewModel.setName("Keisha Grant")

        viewModel.send()

        assertEquals(
            FeedbackSubmission(FeedbackKind.Feedback, "Love it", "sam@example.com", true, Credit.Name, "Keisha Grant"),
            sent.single(),
        )
        assertTrue(viewModel.state.value.sent)
        assertTrue(viewModel.state.value.sentWithEmail)
    }

    @Test
    fun aFailedSendKeepsWhatWasTypedAndSaysWhy() {
        result = SendResult.Unreachable
        val viewModel = viewModel()
        viewModel.setMessage("Show crashed")

        viewModel.send()

        val state = viewModel.state.value
        assertEquals(SendResult.Unreachable, state.failure)
        assertEquals("Show crashed", state.message)
        assertFalse(state.sent)
        assertFalse(state.sending)

        viewModel.setMessage("Show crashed again")
        assertNull(viewModel.state.value.failure)
    }

    @Test
    fun whatWasTypedAndTheThanksSurviveProcessDeath() {
        val savedState = SavedStateHandle()
        viewModel(FeedbackKind.Feedback, savedState).apply {
            setMessage("Love it")
            setEmail("sam@example.com")
            setQuoteConsent(true)
            setCredit(Credit.Initials)
            setName("Keisha Grant")
        }

        val restored = viewModel(FeedbackKind.Feedback, savedState).state.value
        assertEquals("Love it", restored.message)
        assertEquals("sam@example.com", restored.email)
        assertTrue(restored.quoteConsent)
        assertEquals(Credit.Initials, restored.credit)
        assertEquals("Keisha Grant", restored.name)

        viewModel(FeedbackKind.Feedback, savedState).send()
        assertTrue(viewModel(FeedbackKind.Feedback, savedState).state.value.sent)
    }

    @Test
    fun sendingTwiceSendsOnce() {
        val viewModel = viewModel()
        viewModel.setMessage("Hello")

        viewModel.send()
        viewModel.send()

        assertEquals(1, sent.size)
    }
}
