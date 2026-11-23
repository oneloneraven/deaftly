package com.ravendarque.deaftly.ui.savedchats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.ui.chat.FakeSavedChatStore
import com.ravendarque.deaftly.ui.chat.testWriteScope
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class SavedChatsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var store = FakeSavedChatStore()
    private val opened = mutableListOf<String>()

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun keep(
        other: String?,
        vararg lines: String,
    ): String {
        val id = store.newId()
        val chat =
            lines.fold(Conversation(Participant.defaultA(), Participant.defaultB(other))) { c, line ->
                c.append(RichText.spans(line)).withTurn(ParticipantId.B)
            }
        runBlocking { store.save(id, chat) }
        return id
    }

    private fun show() {
        val viewModel = SavedChatsViewModel(store, testWriteScope)
        composeRule.setContent { AppTheme { SavedChatsScreen(viewModel, onOpen = { opened += it }, onBack = {}) } }
        composeRule.waitForIdle()
    }

    private fun date(millis: Long) =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(Locale.UK)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    @Test
    fun showsNameDateTimeAndLastLine() {
        val id = keep("Kwame", "Hello", "See you at 6")
        show()

        composeRule.onNodeWithText("Kwame").assertIsDisplayed()
        composeRule.onNodeWithText("See you at 6").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                date(
                    store.summaries.value
                        .single { it.id == id }
                        .changedAt,
                ),
            ).assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aChatWithNoNameShowsThem() {
        keep(null, "Hello")
        show()

        composeRule.onNodeWithText("Them").assertIsDisplayed()
    }

    @Test
    fun aChatWithNoMessagesShowsNoMessages() {
        keep("Aaliyah")
        show()

        composeRule.onNodeWithText("No messages").assertIsDisplayed()
    }

    @Test
    fun tappingARowOpensThatChat() {
        val id = keep("Kwame", "Hello")
        show()

        composeRule.onNodeWithText("Kwame").performClick()

        assertEquals(listOf(id), opened)
    }

    @Test
    fun deletingAsksFirstThenRemoves() {
        keep("Kwame", "Hello")
        keep("Siobhan", "Hi")
        show()

        composeRule.onNodeWithContentDescription("Delete chat with Kwame").performClick()
        composeRule.onNodeWithText("Delete chat with Kwame?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Kwame").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Delete chat with Kwame").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("Kwame").assertDoesNotExist()
        composeRule.onNodeWithText("Siobhan").assertIsDisplayed()
    }

    @Test
    fun anUnreadableChatSaysSoCannotBeOpenedAndCanBeDeleted() {
        val id = "0f8fad5b-d9cb-469f-a165-70867728950e"
        store = FakeSavedChatStore(unreadable = mapOf(id to 0L))
        show()

        composeRule.onNodeWithText("This chat cannot be read").assertIsDisplayed().performClick()
        assertEquals(emptyList(), opened)
        composeRule.onRoot().tryPerformAccessibilityChecks()

        composeRule.onNodeWithContentDescription("Delete the chat that cannot be read").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("This chat cannot be read").assertDoesNotExist()
    }

    @Test
    fun deletingTheLastChatShowsTheEmptyMessage() {
        keep("Kwame", "Hello")
        show()

        composeRule.onNodeWithContentDescription("Delete chat with Kwame").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("No saved chats. When you end a chat, choose Keep to save it here.").assertIsDisplayed()
    }

    @Test
    fun aChatWhoseFileHasGoneIsDroppedWithAMessage() {
        val id = keep("Kwame", "Hello")
        store.loseFile(id)
        show()

        composeRule.onNodeWithText("Kwame").performClick()

        assertEquals(emptyList(), opened)
        composeRule.onNodeWithText("Kwame").assertDoesNotExist()
        composeRule.onNodeWithText("That chat could not be opened. It may have been deleted.").assertIsDisplayed()
    }

    @Test
    fun theEmptyMessageWaitsUntilSavedChatsHaveBeenRead() {
        store = FakeSavedChatStore(loaded = false)
        show()

        composeRule.onNodeWithText("No saved chats. When you end a chat, choose Keep to save it here.").assertDoesNotExist()
    }
}
