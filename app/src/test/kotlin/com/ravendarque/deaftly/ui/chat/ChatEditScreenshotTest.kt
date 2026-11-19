package com.ravendarque.deaftly.ui.chat

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.chat"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class ChatEditScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private fun show(
        conversation: Conversation,
        dark: Boolean,
        tap: String? = null,
        holdClock: Boolean = false,
    ) {
        val viewModel =
            ChatViewModel(
                FakeSettingsRepository(),
                SavedStateHandle(mapOf("conversation" to ChatCodec.encode(conversation))),
                store = FakeSavedChatStore(),
                writeScope = testWriteScope,
                createSpeechRecogniser = ::silentRecogniser,
                audio = silentAudio(),
            )
        composeRule.setContent { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) } }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = !holdClock
        tap?.let {
            composeRule.onNodeWithContentDescription(it, substring = true).performClick()
            if (holdClock) composeRule.mainClock.advanceTimeBy(NOTICE_SHOWN_MILLIS) else composeRule.waitForIdle()
        }
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun editing(dark: Boolean) = show(orderingConversation(ParticipantId.A), dark, tap = "Sam: Mild please")

    private fun notice(dark: Boolean) = show(orderingConversation(ParticipantId.A), dark, tap = "Waiter: No problem", holdClock = true)

    private fun edited(dark: Boolean) =
        show(
            orderingConversation(ParticipantId.A)
                .replaceMessage(1, RichText.spans("Sure! Is mild or hot better for you?"), null)
                .replaceMessage(2, RichText.spans("Mild please. And no nuts, I'm allergic."), null),
            dark,
        )

    @Test
    fun editingMessageLightTheme() = editing(dark = false)

    @Test
    fun notYoursNoticeLightTheme() = notice(dark = false)

    @Test
    fun notYoursNoticeDarkTheme() = notice(dark = true)

    @Test
    fun editingMessageDarkTheme() = editing(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun editingMessageLargeFontScale() = editing(dark = false)

    @Test
    @Config(qualifiers = "en-rGB-w340dp-h760dp")
    fun editingMessageNarrowScreenDarkTheme() = editing(dark = true)

    @Test
    fun editedLabelLightTheme() = edited(dark = false)

    @Test
    fun editedLabelDarkTheme() = edited(dark = true)
}

private const val NOTICE_SHOWN_MILLIS = 1_000L
