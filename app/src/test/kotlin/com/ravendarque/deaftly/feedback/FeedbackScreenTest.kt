package com.ravendarque.deaftly.feedback

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.CompletableDeferred
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
class FeedbackScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sent = mutableListOf<FeedbackSubmission>()
    private var reply = CompletableDeferred(SendResult.Sent)
    private var done = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(kind: FeedbackKind) {
        val viewModel =
            FeedbackViewModel(kind, {
                sent += it
                reply.await()
            }, SavedStateHandle())
        composeRule.setContent { AppTheme { FeedbackScreen(viewModel, "0.54.1", onDone = { done++ }) } }
        composeRule.waitForIdle()
    }

    @Test
    fun anIssueReportSendsAndThanksWithoutAReply() {
        show(FeedbackKind.Issue)
        composeRule.onNodeWithText("Report an issue").assertIsDisplayed()
        composeRule.onNodeWithText("Can we quote you? (optional)").assertDoesNotExist()
        composeRule.onNodeWithText("We’ll include the app version, 0.54.1, so we know which one you’re using.").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithText("Your message").performTextInput("Show crashed")
        composeRule.onNodeWithText("Send").performClick()

        assertEquals("Show crashed", sent.single().message)
        composeRule.onNodeWithText("Thanks for your message").assertIsDisplayed()
        composeRule.onNodeWithText("You didn’t give an email address, so we can’t reply.").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()
        assertEquals(1, done)
    }

    @Test
    fun feedbackOffersQuotingAndTheNameOnlyWhenItWouldBeShown() {
        show(FeedbackKind.Feedback)
        composeRule.onNodeWithText("Show my name").assertDoesNotExist()

        composeRule.onNodeWithText("Yes, you can quote what I said").performScrollTo().performClick()
        composeRule.onNodeWithText("Don’t show my name").assertExists()
        composeRule.onNodeWithText("Your name").assertDoesNotExist()

        composeRule.onNodeWithText("Show my initials").performScrollTo().performClick()
        composeRule.onNodeWithText("Your name").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun anEmptyMessageSaysWhatToDo() {
        show(FeedbackKind.Issue)

        composeRule.onNodeWithText("Send").performClick()

        composeRule.onNodeWithText("Write a message first.").assertIsDisplayed()
        assertEquals(emptyList(), sent)
    }

    @Test
    fun whileSendingTheButtonIsDisabledAndAFailureKeepsTheMessage() {
        reply = CompletableDeferred()
        show(FeedbackKind.Issue)
        composeRule.onNodeWithText("Your message").performTextInput("Show crashed")
        composeRule.onNodeWithText("Send").performClick()

        composeRule.onNodeWithText("Sending").assertIsNotEnabled()
        reply.complete(SendResult.Unreachable)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("Couldn’t send. Check your connection and try again. Your message is still here.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Show crashed").assertIsDisplayed()
    }
}
