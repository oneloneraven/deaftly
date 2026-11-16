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

    private val store = FakeSavedChatStore()
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
        val viewModel = SavedChatsViewModel(store)
        composeRule.setContent { AppTheme { SavedChatsScreen(viewModel, onOpen = { opened += it }, onBack = {}) } }
        composeRule.waitForIdle()
    }

    private fun date(millis: Long) =
        DateTimeFormatter
            .ofLocalizedDate(
                FormatStyle.MEDIUM,
            ).withLocale(Locale.UK)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    @Test
    fun showsNameDateAndLastLine() {
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
        composeRule.onNodeWithText("Delete this chat?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Kwame").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Delete chat with Kwame").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("Kwame").assertDoesNotExist()
        composeRule.onNodeWithText("Siobhan").assertIsDisplayed()
    }

    @Test
    fun deletingTheLastChatShowsTheEmptyMessage() {
        keep("Kwame", "Hello")
        show()

        composeRule.onNodeWithContentDescription("Delete chat with Kwame").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.onNodeWithText("No saved chats. When you end a chat, choose Keep to save it here.").assertIsDisplayed()
    }
}
