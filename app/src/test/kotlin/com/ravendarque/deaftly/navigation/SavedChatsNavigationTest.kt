package com.ravendarque.deaftly.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.DeaftlyApplication
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LOAD_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class SavedChatsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun waitFor(id: Int) =
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) { composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun aKeptChatAppearsOnHomeAndReopensWithItsMessages() {
        val container = (composeRule.activity.application as DeaftlyApplication).container
        runBlocking { container.settingsRepository.setFirstRunDone() }
        composeRule.setContent { AppTheme { AppNavigation(container) } }
        waitFor(R.string.hub_side_by_side)
        composeRule.onNodeWithText(text(R.string.hub_saved_chats)).assertDoesNotExist()

        composeRule.onNodeWithText(text(R.string.hub_side_by_side)).performClick()
        waitFor(R.string.chat_composer_hint)
        composeRule.onNode(hasSetTextAction()).performTextInput("See you at 6")
        composeRule.onNodeWithContentDescription(text(R.string.chat_send)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        composeRule.onNodeWithText(text(R.string.chat_keep_confirm)).performClick()

        waitFor(R.string.hub_saved_chats)
        composeRule.onNodeWithText(text(R.string.hub_saved_chats)).performScrollTo().performClick()
        waitFor(R.string.saved_chats_them)
        composeRule.onNodeWithText("See you at 6").performClick()

        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText("See you at 6").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText(text(R.string.hub_saved_chats)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription("Me: See you at 6").assertExists()
    }
}
