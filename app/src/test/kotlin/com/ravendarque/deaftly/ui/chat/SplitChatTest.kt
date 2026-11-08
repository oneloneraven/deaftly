package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.settings.ChatLayout
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
class SplitChatTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var ended = 0
    private lateinit var viewModel: ChatViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun showSplit(turn: ParticipantId = ParticipantId.B) {
        val saved =
            SavedStateHandle(
                mapOf("conversation" to ChatCodec.encode(orderingConversation(turn)), "layout" to ChatLayout.Split.ordinal),
            )
        viewModel = ChatViewModel(FakeSettingsRepository(), saved)
        composeRule.setContent { AppTheme { ChatScreen(viewModel, onEnd = { ended++ }, onInsertTemplate = {}) } }
        composeRule.waitForIdle()
    }

    private fun conversation() = (viewModel.state.value as ChatUiState.Ready).conversation

    private fun bounds(description: String): Rect =
        composeRule.onNodeWithContentDescription(description, substring = true).fetchSemanticsNode().boundsInRoot

    private fun halfBounds(label: String): Rect = bounds(label)

    private fun SemanticsNode.collect(): List<SemanticsNode> = listOf(this) + children.flatMap { it.collect() }

    private val otherHalf = "Waiter’s half, upside down for the person opposite"
    private val ownHalf = "Sam’s half"

    @Test
    fun showsTwoHalvesWithTheHearingPersonsAboveTheTurnBarAndTheDeafUsersBelow() {
        showSplit()

        composeRule.onNodeWithContentDescription(otherHalf).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(ownHalf).assertIsDisplayed()
        val top = halfBounds(otherHalf)
        val bottom = halfBounds(ownHalf)
        assertTrue(top.bottom <= bottom.top, "the hearing person's half sits above the deaf user's")
        composeRule.onNodeWithText("WAITER’S TURN").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun eachHalfShowsMessagesFromItsReadersSideWithTheirOwnOnTheirRight() {
        showSplit()
        val width =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.width

        val sams = "Sam: Mild please. And no nuts, I'm allergic."
        val waiters = "Waiter: No problem. It will take about 15 minutes."
        val own = composeRule.onAllNodesWithContentDescription(sams, substring = true).fetchSemanticsNodes()
        val theirs = composeRule.onAllNodesWithContentDescription(waiters, substring = true).fetchSemanticsNodes()

        val samInBottom = own.single { it.boundsInRoot.center.y > halfBounds(ownHalf).top }
        val waiterInBottom = theirs.single { it.boundsInRoot.center.y > halfBounds(ownHalf).top }
        val samInTop = own.single { it.boundsInRoot.center.y < halfBounds(otherHalf).bottom }
        val waiterInTop = theirs.single { it.boundsInRoot.center.y < halfBounds(otherHalf).bottom }
        assertTrue(samInBottom.boundsInRoot.center.x > width / 2, "Sam's own message is on Sam's right")
        assertTrue(waiterInBottom.boundsInRoot.center.x < width / 2, "Waiter's message is on Sam's left")
        assertTrue(waiterInTop.boundsInRoot.center.x < width / 2, "Waiter's own message is on Waiter's right, which is the screen's left")
        assertTrue(samInTop.boundsInRoot.center.x > width / 2, "Sam's message is on Waiter's left, which is the screen's right")
    }

    @Test
    fun theRotatedHalfReadsOldestAtItsTopWhichIsTheScreensBottom() {
        showSplit()
        val topEdge = halfBounds(otherHalf).bottom

        val oldest =
            composeRule
                .onAllNodesWithContentDescription("Sam: Mild please.", substring = true)
                .fetchSemanticsNodes()
                .single { it.boundsInRoot.center.y < topEdge && !it.boundsInRoot.isEmpty }
        val newest =
            composeRule
                .onAllNodesWithContentDescription("Waiter: No problem.", substring = true)
                .fetchSemanticsNodes()
                .single { it.boundsInRoot.center.y < topEdge && !it.boundsInRoot.isEmpty }

        assertTrue(oldest.boundsInRoot.center.y > newest.boundsInRoot.center.y, "oldest is lower on screen, newest nearer the top")
    }

    @Test
    fun onlyTheActivePersonsComposerIsOfferedAndTheOtherHalfWaits() {
        showSplit(ParticipantId.B)

        assertEquals(1, composeRule.onAllNodes(isMessageField).fetchSemanticsNodes().size)
        composeRule.onNodeWithContentDescription("Waiter’s message").assertExists()
        composeRule.onNodeWithContentDescription("Sam’s message").assertDoesNotExist()
        composeRule.onNodeWithText("Waiting for Waiter…").assertIsDisplayed()
        composeRule.onNodeWithText("My turn").assertIsDisplayed().assertWidthIsAtLeast(48.dp)
        composeRule.onNodeWithText("Your turn – type here").assertExists()
    }

    @Test
    fun theActiveComposerHasFocusSoTheKeyboardAppearsThere() {
        showSplit(ParticipantId.A)

        val focused =
            composeRule
                .onNode(isMessageField)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Focused)
        assertEquals(true, focused)
    }

    @Test
    fun tappingMyTurnInTheWaitingHalfSwitchesWhoCanType() {
        showSplit(ParticipantId.B)

        composeRule.onNodeWithText("My turn").performClick()
        composeRule.waitForIdle()

        assertEquals(ParticipantId.A, conversation().turn)
        composeRule.onNodeWithContentDescription("Sam’s message").assertExists()
        composeRule.onNodeWithContentDescription("Waiter’s message").assertDoesNotExist()
        composeRule.onNodeWithText("Waiting for Sam…").assertIsDisplayed()
        composeRule.onNodeWithText("SAM’S TURN").assertIsDisplayed()
    }

    @Test
    fun typingAndSendingAddsAMessageByTheActivePerson() {
        showSplit(ParticipantId.B)

        composeRule.onNode(hasSetTextAction()).performTextInput("Anything else?")
        composeRule.onNodeWithContentDescription("Send").performClick()
        composeRule.waitForIdle()

        val last = conversation().messages.last()
        assertEquals("Anything else?", last.plainText)
        assertEquals(ParticipantId.B, last.author)
        assertEquals(ParticipantId.B, conversation().turn)
    }

    @Test
    fun theEndButtonInTheTurnBarEndsTheChatAfterConfirmation() {
        showSplit()

        composeRule.onNodeWithContentDescription("End chat").assertWidthIsAtLeast(48.dp).performClick()
        assertEquals(0, ended)
        composeRule.onNodeWithText("The messages will be deleted.").assertIsDisplayed()
        composeRule.onNodeWithText("End chat").performClick()

        assertEquals(1, ended)
    }

    @Test
    fun messagesCannotBeQuotedFromTheCompactHalves() {
        showSplit()

        val actions =
            composeRule
                .onAllNodesWithContentDescription("Sam: Mild please.", substring = true)
                .fetchSemanticsNodes()
                .map { it.config.getOrNull(SemanticsActions.CustomActions).orEmpty() }
        assertTrue(actions.all { it.isEmpty() })
    }

    @Test
    fun screenReadersGetTheRotatedHalfFirstThenTheTurnBarThenTheOtherHalfAndEachHalfMessagesBeforeItsComposer() {
        showSplit()
        val root = composeRule.onRoot().fetchSemanticsNode().collect()

        fun SemanticsNode.label() = config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

        fun SemanticsNode.index() = config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f

        fun SemanticsNode.isMessage() =
            label()?.let { it.startsWith("Sam: ") || it.startsWith("Waiter: ") || it.startsWith("Replying to") } == true
        val top = root.single { it.label() == otherHalf }
        val bottom = root.single { it.label() == ownHalf }
        val bar =
            root.first { bar ->
                bar.index() == 1f &&
                    bar.collect().any { it.config.getOrNull(SemanticsProperties.Text)?.any { t -> "TURN" in t.text } == true }
            }

        assertTrue(top.index() < bar.index() && bar.index() < bottom.index())
        assertEquals(true, top.config.getOrNull(SemanticsProperties.IsTraversalGroup))
        assertEquals(true, bottom.config.getOrNull(SemanticsProperties.IsTraversalGroup))
        listOf(top, bottom).forEach { half ->
            val order = half.collect()
            val messages = order.filter { it.isMessage() }
            assertEquals(4, messages.size)
            assertTrue(
                order.indexOf(messages.last()) <
                    order.indexOfFirst {
                        it.config.contains(SemanticsProperties.EditableText) || it.label()?.startsWith("Waiting") == true ||
                            it.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text.startsWith("Waiting") } == true
                    },
            )
            assertEquals(messages.map { it.index() }.sorted(), messages.map { it.index() }, "traversal indices follow chronology")
            assertEquals(4, messages.map { it.index() }.distinct().size)
        }
    }

    @Test
    fun turnBarAndHalvesPassAccessibilityChecksAtTheOtherTurn() {
        showSplit(ParticipantId.A)

        composeRule.onNodeWithText("My turn").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test
    fun messageTextIsAvailableToScreenReadersInTheRotatedHalfToo() {
        showSplit()

        assertEquals(
            2,
            composeRule.onAllNodesWithContentDescription("Waiter: Sure! Do you want it spicy or mild?").fetchSemanticsNodes().size,
        )
        assertEquals(0, composeRule.onAllNodesWithText("Waiting for Sam…").fetchSemanticsNodes().size)
    }
}
