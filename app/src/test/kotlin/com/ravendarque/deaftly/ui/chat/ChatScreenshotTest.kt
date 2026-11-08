package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.components.SheetBody
import com.ravendarque.deaftly.ui.components.SheetHandle
import com.ravendarque.deaftly.ui.theme.AppTheme
import com.ravendarque.deaftly.ui.theme.Deaftly
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
@Config(qualifiers = "w390dp-h844dp")
class ChatScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private val defaults = Conversation(Participant.defaultA(), Participant.defaultB())
    private val named =
        Conversation(
            Participant(ParticipantId.A, ColourRole.Teal, "Alex"),
            Participant(ParticipantId.B, ColourRole.Berry, "Sam"),
        )

    private fun shot(content: @Composable () -> Unit) {
        composeRule.setContent { content() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun sheet(
        conversation: Conversation,
        dark: Boolean,
    ) = shot {
        AppTheme(darkTheme = dark) {
            Surface(color = Deaftly.colors.surface, shape = Deaftly.shapes.sheet) {
                Column {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
                    SheetBody("Who is chatting?") { ParticipantsSheetContent(conversation, onChange = {}, onDone = {}) }
                }
            }
        }
    }

    private fun header(
        conversation: Conversation,
        dark: Boolean,
    ) = shot {
        AppTheme(darkTheme = dark) {
            Surface { ChatHeader(conversation, onEnd = {}, onEditParticipants = {}, textSize = ChatTextSize.Normal, onCycleTextSize = {}) }
        }
    }

    private fun screen(
        turn: ParticipantId,
        dark: Boolean,
        draft: String = "",
    ) {
        val saved =
            SavedStateHandle(
                mapOf("conversation" to ChatCodec.encode(orderingConversation(turn)), "draft_a" to draft, "draft_b" to draft),
            )
        val viewModel = ChatViewModel(FakeSettingsRepository(), saved)
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}) } }
    }

    private fun formatted(dark: Boolean) {
        val saved =
            SavedStateHandle(
                mapOf(
                    "conversation" to ChatCodec.encode(formattingConversation()),
                    "draft_b" to ChatCodec.encodeDraft(formattingDraft()),
                ),
            )
        val viewModel = ChatViewModel(FakeSettingsRepository(), saved)
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}) } }
    }

    private fun continuedFromShow(dark: Boolean) {
        val viewModel = ChatViewModel(FakeSettingsRepository(), SavedStateHandle(), "Hi! I'm deaf. Could I have a table for two, please?")
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}) } }
    }

    @Test
    fun formattingAndQuotesLightTheme() = formatted(dark = false)

    @Test
    fun formattingAndQuotesDarkTheme() = formatted(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun formattingAndQuotesLargeFontScale() = formatted(dark = false)

    @Test
    fun continuedFromShowLightTheme() = continuedFromShow(dark = false)

    @Test
    fun continuedFromShowDarkTheme() = continuedFromShow(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun continuedFromShowLargeFontScale() = continuedFromShow(dark = false)

    @Test
    fun conversationHearingPersonsTurnLightTheme() = screen(ParticipantId.B, dark = false)

    @Test
    fun conversationHearingPersonsTurnDarkTheme() = screen(ParticipantId.B, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun conversationHearingPersonsTurnLargeFontScale() = screen(ParticipantId.B, dark = false)

    @Test
    fun conversationDeafUsersTurnLightTheme() = screen(ParticipantId.A, dark = false, draft = "Thank you")

    @Test
    fun conversationDeafUsersTurnDarkTheme() = screen(ParticipantId.A, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun conversationDeafUsersTurnLargeFontScale() = screen(ParticipantId.A, dark = false)

    @Test
    fun sheetWithDefaultsLightTheme() = sheet(defaults, dark = false)

    @Test
    fun sheetWithDefaultsDarkTheme() = sheet(defaults, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun sheetWithDefaultsLargeFontScale() = sheet(defaults, dark = false)

    @Test
    fun sheetWithNamesLightTheme() = sheet(named, dark = false)

    @Test
    fun sheetWithNamesDarkTheme() = sheet(named, dark = true)

    @Test
    fun headerWithDefaultsLightTheme() = header(defaults, dark = false)

    @Test
    fun headerWithNamesDarkTheme() = header(named, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun headerWithNamesLargeFontScale() = header(named, dark = false)
}
