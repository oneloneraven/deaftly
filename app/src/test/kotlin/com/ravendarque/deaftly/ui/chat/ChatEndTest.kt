package com.ravendarque.deaftly.ui.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.Conversation
import kotlinx.coroutines.runBlocking
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
class ChatEndTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var store = FakeSavedChatStore()
    private var ended = 0
    private lateinit var viewModel: ChatViewModel

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(
        conversation: Conversation? = orderingConversation(),
        savedChatId: String? = null,
    ) {
        val saved = SavedStateHandle(listOfNotNull(conversation?.let { "conversation" to ChatCodec.encode(it) }).toMap())
        viewModel =
            ChatViewModel(
                FakeSettingsRepository(),
                saved,
                ChatOpening(savedChatId = savedChatId),
                store = store,
                writeScope = testWriteScope,
            )
        composeRule.setContent {
            com.ravendarque.deaftly.ui.theme.AppTheme {
                ChatScreen(
                    viewModel,
                    onEnd = { ended++ },
                    onInsertTemplate = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun endChat() = composeRule.onNodeWithContentDescription("End chat").performClick()

    private fun systemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun conversation() = (viewModel.state.value as ChatUiState.Ready).conversation

    @Test
    fun endingAChatWithMessagesOffersKeepDeleteAndKeepChatting() {
        show()

        endChat()

        composeRule.onNodeWithText("Keep this chat?").assertIsDisplayed()
        listOf("Keep", "Delete", "Keep chatting").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onRoot().tryPerformAccessibilityChecks()
        assertEquals(0, ended)
    }

    @Test
    fun keepSavesAndEnds() {
        show()
        endChat()

        composeRule.onNodeWithText("Keep").performClick()

        assertEquals(1, store.saves)
        assertEquals(
            orderingConversation(),
            runBlocking {
                store.load(
                    store.summaries.value
                        .single()
                        .id,
                )
            }?.conversation,
        )
        assertEquals(1, ended)
    }

    @Test
    fun deleteEndsWithoutSaving() {
        show()
        endChat()

        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(0, store.saves)
        assertEquals(1, ended)
    }

    @Test
    fun keepChattingClosesTheDialogOnly() {
        show()
        endChat()

        composeRule.onNodeWithText("Keep chatting").performClick()

        composeRule.onNodeWithText("Keep this chat?").assertDoesNotExist()
        assertEquals(0, ended)
        assertEquals(4, conversation().messages.size)
    }

    @Test
    fun endingAKeptChatLeavesWithoutAskingOrRewritingIt() {
        val id = store.newId()
        runBlocking { store.save(id, orderingConversation()) }
        show(conversation = null, savedChatId = id)

        endChat()

        composeRule.onNodeWithText("Keep this chat?").assertDoesNotExist()
        assertEquals(1, ended)
        assertEquals(1, store.saves)
    }

    @Test
    fun aFailedKeepStaysInTheChatAndSaysSo() {
        store = FakeSavedChatStore(failSaves = true)
        show()
        endChat()

        composeRule.onNodeWithText("Keep").performClick()

        composeRule.onNodeWithText("This chat couldn\u2019t be saved. Try Keep again, or Delete to end without saving.").assertIsDisplayed()
        assertEquals(0, ended)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun endingAKeptChatWithNoMessagesSavesWithoutAsking() {
        val id = store.newId()
        runBlocking { store.save(id, orderingConversation()) }
        show(conversation = null, savedChatId = id)
        repeat(4) { viewModel.deleteMessage(0) }
        composeRule.waitForIdle()

        endChat()

        assertEquals(1, ended)
        assertEquals(emptyList(), runBlocking { store.load(id) }?.conversation?.messages)
    }

    @Test
    fun endingAnEmptyChatAsksNothingAndKeepsNothing() {
        show(conversation = null)

        endChat()

        assertEquals(1, ended)
        assertEquals(0, store.saves)
    }

    @Test
    fun systemBackAsksToKeepAChatWithMessages() {
        show()

        systemBack()

        composeRule.onNodeWithText("Keep this chat?").assertIsDisplayed()
        assertEquals(0, ended)
    }

    @Test
    fun systemBackLeavesAnEmptyChatWithoutAsking() {
        show(conversation = null)

        systemBack()

        composeRule.onNodeWithText("Keep this chat?").assertDoesNotExist()
        assertEquals(1, ended)
    }
}
