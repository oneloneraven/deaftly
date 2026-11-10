package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.settings.ChatTextSize
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
            Participant(ParticipantId.A, ColourRole.Green, "Alex"),
            Participant(ParticipantId.B, ColourRole.Blue, "Sam"),
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

    private fun header(dark: Boolean) =
        shot {
            AppTheme(darkTheme = dark) {
                Surface { ChatHeader(ChatTextSize.Medium, ChatHeaderActions({}, {}, {}, {})) }
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
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) } }
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
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) } }
    }

    private fun script(
        dark: Boolean,
        size: ChatTextSize = ChatTextSize.Medium,
        swapped: Boolean = false,
        turn: ParticipantId = ParticipantId.A,
        them: ColourRole = ColourRole.Gold,
        me: ColourRole = ColourRole.Blue,
    ) {
        val saved = SavedStateHandle(mapOf("conversation" to ChatCodec.encode(scriptConversation(them, me, turn))))
        val viewModel = ChatViewModel(FakeSettingsRepository(textSize = size, sidesSwapped = swapped), saved)
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) } }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun messageMenu(
        dark: Boolean,
        speaker: String,
        swapped: Boolean = false,
    ) {
        val saved = SavedStateHandle(mapOf("conversation" to ChatCodec.encode(scriptConversation(turn = ParticipantId.A))))
        val viewModel = ChatViewModel(FakeSettingsRepository(sidesSwapped = swapped), saved)
        composeRule.setContent { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) } }
        composeRule.waitForIdle()
        composeRule
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions) and hasContentDescription("$speaker:", substring = true),
            ).onFirst()
            .performTouchInput { longClick() }
        composeRule.waitForIdle()
        captureScreenRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun switchOnly(
        turn: ParticipantId,
        dark: Boolean,
        swapped: Boolean = false,
    ) = shot {
        AppTheme(darkTheme = dark) {
            Surface {
                TurnControl(
                    conversation = scriptConversation(turn = turn),
                    sidesSwapped = swapped,
                    onSelect = {},
                    modifier = Modifier.padding(Deaftly.spacing.space12),
                )
            }
        }
    }

    private fun continuedFromShow(dark: Boolean) {
        val viewModel =
            ChatViewModel(
                FakeSettingsRepository(),
                SavedStateHandle(),
                ChatOpening(listOf("Hi! I'm deaf. Could I have a table for two, please?")),
            )
        shot { AppTheme(darkTheme = dark) { ChatScreen(viewModel, onEnd = {}, onInsertTemplate = {}) } }
    }

    @Test
    fun messageMenuLeftSpeakerLightTheme() = messageMenu(dark = false, speaker = "Waiter")

    @Test
    fun messageMenuLeftSpeakerDarkTheme() = messageMenu(dark = true, speaker = "Waiter")

    @Test
    @Config(fontScale = 2f)
    fun messageMenuLeftSpeakerLargeFontScale() = messageMenu(dark = false, speaker = "Waiter")

    @Test
    fun messageMenuRightSpeakerLightTheme() = messageMenu(dark = false, speaker = "Sam")

    @Test
    fun messageMenuRightSpeakerDarkTheme() = messageMenu(dark = true, speaker = "Sam")

    @Test
    @Config(fontScale = 2f)
    fun messageMenuRightSpeakerLargeFontScale() = messageMenu(dark = false, speaker = "Sam")

    @Test
    fun messageMenuSwappedSidesLightTheme() = messageMenu(dark = false, speaker = "Waiter", swapped = true)

    @Test
    fun scriptLightTheme() = script(dark = false)

    @Test
    fun scriptDarkTheme() = script(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun scriptLargeFontScale() = script(dark = false)

    @Test
    fun scriptSwappedLightTheme() = script(dark = false, swapped = true, them = ColourRole.Green, me = ColourRole.Indigo)

    @Test
    fun scriptSwappedDarkTheme() = script(dark = true, swapped = true, them = ColourRole.Green, me = ColourRole.Indigo)

    @Test
    @Config(fontScale = 2f)
    fun scriptSwappedLargeFontScale() = script(dark = false, swapped = true, them = ColourRole.Green, me = ColourRole.Indigo)

    @Test
    fun scriptSmallTextLightTheme() = script(dark = false, size = ChatTextSize.Small)

    @Test
    fun scriptSmallTextDarkTheme() = script(dark = true, size = ChatTextSize.Small)

    @Test
    @Config(fontScale = 2f)
    fun scriptSmallTextLargeFontScale() = script(dark = false, size = ChatTextSize.Small)

    @Test
    fun scriptLargeTextLightTheme() = script(dark = false, size = ChatTextSize.Large)

    @Test
    fun scriptLargeTextDarkTheme() = script(dark = true, size = ChatTextSize.Large)

    @Test
    @Config(fontScale = 2f)
    fun scriptLargeTextLargeFontScale() = script(dark = false, size = ChatTextSize.Large)

    @Test
    fun switchTurnOnRightLightTheme() = switchOnly(ParticipantId.A, dark = false)

    @Test
    fun switchTurnOnRightDarkTheme() = switchOnly(ParticipantId.A, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun switchTurnOnRightLargeFontScale() = switchOnly(ParticipantId.A, dark = false)

    @Test
    fun switchTurnOnLeftLightTheme() = switchOnly(ParticipantId.B, dark = false)

    @Test
    fun scriptTurnOnLeftLightTheme() = script(dark = false, turn = ParticipantId.B)

    @Test
    fun switchTurnOnLeftDarkTheme() = switchOnly(ParticipantId.B, dark = true)

    @Test
    fun scriptTurnOnLeftDarkTheme() = script(dark = true, turn = ParticipantId.B)

    @Test
    @Config(fontScale = 2f)
    fun switchTurnOnLeftLargeFontScale() = switchOnly(ParticipantId.B, dark = false)

    @Test
    @Config(fontScale = 2f)
    fun scriptTurnOnLeftLargeFontScale() = script(dark = false, turn = ParticipantId.B)

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
    fun headerLightTheme() = header(dark = false)

    @Test
    fun headerDarkTheme() = header(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun headerLargeFontScale() = header(dark = false)
}
