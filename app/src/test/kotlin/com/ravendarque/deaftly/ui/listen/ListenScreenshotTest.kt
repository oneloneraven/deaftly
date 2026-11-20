package com.ravendarque.deaftly.ui.listen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.ui.chat.VoicePermissionPrompt
import com.ravendarque.deaftly.ui.chat.VoiceTypingState
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class ListenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val heard =
        listOf(
            "Hi, welcome in. Is it just the two of you tonight?",
            "Lovely. We have a table by the window if you'd like it.",
        )

    private val listening = VoiceTypingState(on = true, listening = true, live = "Can I get you any drinks while")

    private fun capture(
        dark: Boolean = false,
        state: ListenUiState = ListenUiState(heard, listening),
        prompt: VoicePermissionPrompt? = null,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = dark) {
                ListenContent(state, prompt, ListenActions({}, {}, {}, {}, { state.heard.isEmpty() }))
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun lightTheme() = capture()

    @Test
    fun darkTheme() = capture(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun largeFontScale() = capture()

    @Test
    fun signFacingYouLightTheme() = capture(state = ListenUiState(heard, listening, signFacesThem = false))

    @Test
    fun largeTextLightTheme() = capture(state = ListenUiState(heard, listening, ChatTextSize.Large))

    @Test
    fun waitingLightTheme() = capture(state = ListenUiState(voice = VoiceTypingState(on = true, listening = true)))

    @Test
    fun pausedDarkTheme() = capture(dark = true, state = ListenUiState(heard, VoiceTypingState(on = false)))

    @Test
    fun unavailableLightTheme() = capture(state = ListenUiState(voice = VoiceTypingState(unavailable = true)))

    @Test
    fun permissionLightTheme() = capture(state = ListenUiState(), prompt = VoicePermissionPrompt(blocked = false, {}, {}))

    @Test
    fun permissionBlockedDarkTheme() = capture(dark = true, state = ListenUiState(), prompt = VoicePermissionPrompt(blocked = true, {}, {}))
}
