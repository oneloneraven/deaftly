package com.ravendarque.deaftly.ui.facetoface

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.ui.theme.AppTheme
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
@Config(qualifiers = "w390dp-h844dp")
class FaceToFaceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val messages = mutableStateListOf<String>()
    private val field = TextFieldState()
    private var ends = 0
    private var continues = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(initial: List<String> = emptyList()) {
        messages.addAll(initial)
        composeRule.setContent {
            AppTheme {
                FaceToFaceContent(
                    messages = messages,
                    field = field,
                    onSend = { text ->
                        messages += text.trim()
                        field.edit { delete(0, length) }
                    },
                    onEnd = { ends++ },
                    onContinue = { continues++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun input() = composeRule.onNode(hasSetTextAction())

    private fun send() = composeRule.onNodeWithContentDescription("Send")

    private fun strangerList() = composeRule.onNodeWithContentDescription("Your messages, upside down for the person opposite")

    private fun SemanticsNodeInteraction.traversalIndex(): Float {
        var node: SemanticsNode? = fetchSemanticsNode()
        while (node != null) {
            node.config.getOrNull(SemanticsProperties.TraversalIndex)?.let { return it }
            node = node.parent
        }
        error("No traversal index above the node")
    }

    private fun top(text: String) =
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun liveTop(text: String) =
        composeRule
            .onNodeWithContentDescription("Typing now: $text")
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun isInView(text: String) = runCatching { composeRule.onNodeWithText(text).assertIsDisplayed() }.isSuccess

    @Test
    fun showsAWelcomeForTheStrangerBeforeAnythingIsTyped() {
        show()

        composeRule.onNodeWithText("Hello. I’m deaf, so I’m typing to you. Read along here.").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun sentMessagesAreShownInOrderRotatedSoTheOldestIsNearestTheDeafUser() {
        show(listOf("First", "Second", "Third"))

        assertTrue(top("First") > top("Second"))
        assertTrue(top("Second") > top("Third"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theTextBeingTypedAppearsLiveBelowTheSentMessages() {
        show(listOf("Hello"))

        input().performTextInput("Could I")
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Typing now: Could I").assertIsDisplayed()
        assertTrue(top("Hello") > liveTop("Could I"))

        input().performTextInput(" have a coffee")
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Typing now: Could I have a coffee").assertIsDisplayed()
    }

    @Test
    fun theLiveTextDisappearsWhenTheFieldIsEmpty() {
        show()

        composeRule.onNodeWithContentDescription("Typing now: ", substring = true).assertDoesNotExist()
        input().performTextInput("Hi")
        composeRule.onNodeWithContentDescription("Typing now: Hi").assertIsDisplayed()
    }

    @Test
    fun sendAddsTheTextToTheHistoryClearsTheFieldAndKeepsTheFocus() {
        show()
        input().assertIsFocused()

        input().performTextInput("Hello, I am deaf")
        send().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("Hello, I am deaf"), messages.toList())
        assertEquals("", field.text.toString())
        input().assertIsFocused()
        composeRule.onNodeWithText("Hello, I am deaf").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Typing now: ", substring = true).assertDoesNotExist()
    }

    @Test
    fun sendIsDisabledWhileTheFieldIsBlank() {
        show()

        send().assertIsNotEnabled()
        input().performTextInput("   ")
        send().assertIsNotEnabled()
        input().performTextInput("Hi")
        send().assertIsEnabled()
    }

    private fun assertSendFillsTheFieldsRightEdge() {
        val frame = send().onParent().fetchSemanticsNode().boundsInRoot
        val sendBounds = send().fetchSemanticsNode().boundsInRoot
        val border = with(composeRule.density) { 2.dp.toPx() }
        assertEquals(frame.top + border, sendBounds.top, 0.5f, "top")
        assertEquals(frame.bottom - border, sendBounds.bottom, 0.5f, "bottom")
        assertEquals(frame.right - border, sendBounds.right, 0.5f, "right")
        send().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun theHintFitsOnOneLineOnANarrowScreen() {
        show()

        val height =
            composeRule
                .onNodeWithText("Type your message…", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.height
        assertTrue(height < with(composeRule.density) { 30.dp.toPx() }, "hint wrapped")
    }

    @Test
    fun theSendButtonIsFlushWithTheFieldsBorderOnTopRightAndBottom() {
        show()

        assertSendFillsTheFieldsRightEdge()
    }

    @Test
    fun theSendButtonStaysFlushAsTheFieldGrowsToItsLimit() {
        show()
        val before = send().fetchSemanticsNode().boundsInRoot.height

        input().performTextInput("one two three four five six seven eight nine ten ".repeat(8))
        composeRule.waitForIdle()

        assertTrue(send().fetchSemanticsNode().boundsInRoot.height > before)
        assertSendFillsTheFieldsRightEdge()
    }

    @Test
    fun theNewestMessageStaysInViewAndTheOldestScrollsAway() {
        show((1..40).map { "Message $it" })

        assertTrue(isInView("Message 40"))
        assertTrue(!isInView("Message 1"))
    }

    @Test
    fun theLiveTextKeepsTheNewestContentInViewAsItGrows() {
        show((1..40).map { "Message $it" })

        input().performTextInput("A long reply that keeps on going and going so that it wraps over several lines for the reader")
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Typing now: ", substring = true).assertIsDisplayed()
    }

    @Test
    fun scrollingTheStrangersHalfKeepsTheFieldFocusedAndScrollsBackThroughMessages() {
        show((1..40).map { "Message $it" })
        input().performTextInput("Still typing")
        input().assertIsFocused()

        repeat(2) { strangerList().performTouchInput { swipeDown(startY = height * 0.1f, endY = height * 0.9f, durationMillis = 60) } }
        composeRule.waitForIdle()

        input().assertIsFocused()
        assertTrue(!isInView("Message 40"))
        assertEquals("Still typing", field.text.toString())
    }

    @Test
    fun tappingTheStrangersHalfDoesNotTakeTheFocus() {
        show(listOf("Hello"))
        input().assertIsFocused()

        strangerList().performTouchInput { click(Offset(width * 0.5f, height * 0.5f)) }
        composeRule.waitForIdle()

        input().assertIsFocused()
    }

    @Test
    fun talkBackReadsTheDeafUsersHalfFirstThenTheStrangersMessagesThenTheTopBar() {
        show(listOf("Hello"))

        val field = input().traversalIndex()
        val list = strangerList().traversalIndex()
        val bar = composeRule.onNodeWithContentDescription("End face to face").traversalIndex()
        val continueButton = composeRule.onNodeWithText("Continue side by side").traversalIndex()

        assertTrue(field < list, "field $field before list $list")
        assertTrue(list < bar, "list $list before bar $bar")
        assertEquals(bar, continueButton)
    }

    @Test
    fun theHeaderBarSitsBetweenTheStrangersPaneAndTheFieldAndThePaneRunsToTheTopEdge() {
        show(listOf("Hello"))

        val list = strangerList().fetchSemanticsNode().boundsInRoot
        val bar = composeRule.onNodeWithText("Continue side by side").fetchSemanticsNode().boundsInRoot
        val field = input().fetchSemanticsNode().boundsInRoot

        assertEquals(0f, list.top)
        assertTrue(list.bottom <= bar.top, "pane ${list.bottom} above bar ${bar.top}")
        assertTrue(bar.bottom <= field.top, "bar ${bar.bottom} above field ${field.top}")
    }

    @Test
    fun theStrangersHalfIsAListAndNothingIsALiveRegion() {
        show(listOf("Hello"))
        input().performTextInput("Typing")

        strangerList().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertCountEquals(0)
    }

    @Test
    fun closingWithNothingSentLeavesStraightAway() {
        show()

        composeRule.onNodeWithContentDescription("End face to face").performClick()

        assertEquals(1, ends)
        composeRule.onNodeWithText("End this chat?").assertDoesNotExist()
    }

    @Test
    fun closingWithMessagesAsksFirstAndKeepingGoesBack() {
        show(listOf("Hello"))

        composeRule.onNodeWithContentDescription("End face to face").performClick()
        composeRule.onNodeWithText("End this chat?").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithText("Keep chatting").performClick()

        assertEquals(0, ends)
        composeRule.onNodeWithText("End this chat?").assertDoesNotExist()
    }

    @Test
    fun confirmingTheEndLeaves() {
        show(listOf("Hello"))

        composeRule.onNodeWithContentDescription("End face to face").performClick()
        composeRule.onNodeWithText("End chat").performClick()

        assertEquals(1, ends)
    }

    @Test
    fun continueSideBySideIsReported() {
        show(listOf("Hello"))

        composeRule.onNodeWithText("Continue side by side").performClick()

        assertEquals(1, continues)
    }
}
