package com.ravendarque.deaftly.ui.listen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.chat.VoiceTypingState
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class ListenScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private class Calls {
        var ended = 0
        var flipped = 0
        var toggled = 0
        var continued = 0
    }

    private fun show(state: ListenUiState): Calls {
        val calls = Calls()
        composeRule.setContent {
            AppTheme {
                ListenContent(
                    state,
                    null,
                    ListenActions(
                        onEnd = { calls.ended++ },
                        onFlip = { calls.flipped++ },
                        onToggleListening = { calls.toggled++ },
                        onContinue = { calls.continued++ },
                        isEmpty = { state.heard.isEmpty() && state.voice.live.isBlank() },
                    ),
                )
            }
        }
        return calls
    }

    @Test
    fun endingWithNothingHeardLeavesStraightAway() {
        val calls = show(ListenUiState(voice = VoiceTypingState(on = true)))

        composeRule.onNodeWithContentDescription(text(R.string.listen_close)).performClick()

        assertEquals(1, calls.ended)
    }

    @Test
    fun endingAfterTheySpokeAsksFirst() {
        val calls = show(ListenUiState(listOf("Table for two?"), VoiceTypingState(on = true)))

        composeRule.onNodeWithContentDescription(text(R.string.listen_close)).performClick()
        composeRule.onNodeWithText(text(R.string.listen_end_keep)).performClick()
        assertEquals(0, calls.ended)

        composeRule.onNodeWithContentDescription(text(R.string.listen_close)).performClick()
        composeRule.onNodeWithText(text(R.string.listen_end_confirm)).performClick()
        assertEquals(1, calls.ended)
    }

    @Test
    fun controlsReachTheirActions() {
        val calls = show(ListenUiState(voice = VoiceTypingState(on = true)))

        composeRule.onNodeWithContentDescription(text(R.string.listen_flip_to_you)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.listen_pause)).performClick()
        composeRule.onNodeWithText(text(R.string.f2f_continue)).performClick()

        assertEquals(1, calls.flipped)
        assertEquals(1, calls.toggled)
        assertEquals(1, calls.continued)
    }

    @Test
    fun listenIsOfferedWhenPausedAndDisabledWhenUnavailable() {
        show(ListenUiState(voice = VoiceTypingState(unavailable = true)))

        composeRule.onNodeWithContentDescription(text(R.string.listen_resume)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.listen_unavailable)).assertExists()
    }
}
