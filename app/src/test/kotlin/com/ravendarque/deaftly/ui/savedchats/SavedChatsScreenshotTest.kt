package com.ravendarque.deaftly.ui.savedchats

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.storage.SavedChat
import com.ravendarque.deaftly.ui.chat.FakeSavedChatStore
import com.ravendarque.deaftly.ui.chat.testWriteScope
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.savedchats"
private const val NOON_17_NOV_2026 = 1_794_916_800_000L
private const val DAY = 86_400_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class SavedChatsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private val zone = TimeZone.getDefault()

    @Before
    fun pinTheTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
    }

    @After
    fun restoreTheTimeZone() {
        TimeZone.setDefault(zone)
    }

    private fun chat(
        id: String,
        other: String?,
        colour: ColourRole,
        daysAgo: Int,
        vararg lines: String,
    ): SavedChat {
        val conversation =
            lines.fold(Conversation(Participant.defaultA(), Participant(ParticipantId.B, colour, other))) { c, line ->
                c.append(RichText.spans(line)).switchTurn()
            }
        return SavedChat(id, conversation, NOON_17_NOV_2026 - daysAgo * DAY)
    }

    private val chats =
        listOf(
            chat(
                "0f8fad5b-d9cb-469f-a165-70867728950e",
                "Kwame",
                ColourRole.Green,
                0,
                "Are you still coming tonight?",
                "Yes! See you at 6 by the station entrance.",
            ),
            chat("7c9e6679-7425-40de-944b-e07fc1f90ae7", "Aaliyah", ColourRole.Blue, 2, "Lunch on Friday?"),
            chat(
                "16fd2706-8baf-433b-82eb-8c7fada847da",
                null,
                ColourRole.Red,
                9,
                "Your appointment is at 10:40 on Tuesday, please arrive early.",
            ),
            chat("6ecd8c99-4036-403d-bf84-cf8400f67836", "Siobhan", ColourRole.Gold, 30),
        )

    private fun shot(
        dark: Boolean,
        saved: List<SavedChat> = chats,
        unreadable: Map<String, Long> = emptyMap(),
    ) {
        val viewModel = SavedChatsViewModel(FakeSavedChatStore(saved, unreadable = unreadable), testWriteScope)
        composeRule.setContent { AppTheme(darkTheme = dark) { SavedChatsScreen(viewModel, onOpen = {}, onBack = {}) } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    @Test
    fun listLightTheme() = shot(dark = false)

    @Test
    fun listDarkTheme() = shot(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun listLargeFontScale() = shot(dark = false)

    @Test
    fun emptyLightTheme() = shot(dark = false, saved = emptyList())

    @Test
    fun withUnreadableLightTheme() =
        shot(dark = false, saved = chats.take(2), unreadable = mapOf("c56a4180-65aa-42ec-a945-5fd21dec0538" to NOON_17_NOV_2026 - 40 * DAY))
}
