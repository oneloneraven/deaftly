package com.ravendarque.deaftly.ui.show

import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.core.settings.ShowTextSize
import com.ravendarque.deaftly.navigation.HighlightRange
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SHORT = "Hi! I'm deaf. I'd like to check in. My name is Sam. Please type on my phone."
private const val MAX_PAGES = 100
private val LONG = List(150) { "Could I have the pasta arrabbiata please? I am deaf." }.joinToString(" ")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class ShowScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var closes = 0
    private var chats = 0
    private var scheme by mutableStateOf(ShowScheme.BlackOnWhite)

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(
        text: String,
        darkTheme: Boolean = false,
        highlights: List<HighlightRange> = emptyList(),
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                ShowContent(
                    text,
                    scheme,
                    onCycleScheme = { scheme = scheme.next() },
                    onClose = { closes++ },
                    onContinueAsChat = { chats++ },
                    highlights = highlights,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Capture(onView: (View) -> Unit) {
        onView(LocalView.current)
    }

    @Test
    fun showsTheMessageTheChatButtonAndTheHint() {
        show(SHORT)

        composeRule.onNodeWithText(SHORT).assertExists()
        composeRule.onNodeWithText("Continue as chat").assertExists()
        composeRule.onNodeWithText("Swipe down to go back").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun passesAccessibilityChecksInTheDarkTheme() {
        show(SHORT, darkTheme = true)

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun emphasisedTextPassesAccessibilityChecksInEveryScheme() {
        val start = SHORT.indexOf("type on my phone")
        val highlights = listOf(HighlightRange(start, start + "type on my phone".length))
        show(SHORT, highlights = highlights)
        ShowScheme.entries.forEach { current ->
            scheme = current
            composeRule.waitForIdle()
            composeRule.onNodeWithText(SHORT).assertExists()
            composeRule.onRoot().tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun theColourButtonCyclesTheSchemeAndNamesCurrentAndNext() {
        show(SHORT)

        composeRule.onNodeWithContentDescription("Change colours. Now black on white, next white on black").performClick()
        composeRule.onNodeWithContentDescription("Change colours. Now white on black, next yellow on black").performClick()
        composeRule.onNodeWithContentDescription("Change colours. Now yellow on black, next black on white").assertExists()
        composeRule.onRoot().tryPerformAccessibilityChecks()
        assertEquals(ShowScheme.YellowOnBlack, scheme)
    }

    @Test
    fun closeButtonLeaves() {
        show(SHORT)

        composeRule.onNodeWithContentDescription("Close show mode").performClick()

        assertEquals(1, closes)
    }

    @Test
    fun continueAsChatIsReported() {
        show(SHORT)

        composeRule.onNodeWithText("Continue as chat").performClick()

        assertEquals(1, chats)
    }

    @Test
    fun swipingDownLeaves() {
        show(SHORT)

        composeRule.onRoot().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.7f) }

        assertEquals(1, closes)
    }

    @Test
    fun swipingDownLeavesWhenLongTextIsScrolledToTheTop() {
        show(LONG)

        composeRule.onRoot().performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.7f) }

        assertEquals(1, closes)
    }

    @Test
    fun swipingUpDoesNotLeave() {
        show(SHORT)

        composeRule.onRoot().performTouchInput { swipeUp() }

        assertEquals(0, closes)
    }

    @Test
    fun keepsTheScreenAwakeWhileShown() {
        var view: View? = null
        var shown by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                Capture { view = it }
                if (shown) ShowContent(SHORT, ShowScheme.BlackOnWhite, onCycleScheme = {}, onClose = {}, onContinueAsChat = {})
            }
        }
        composeRule.waitForIdle()
        assertTrue(view!!.keepScreenOn)

        shown = false
        composeRule.waitForIdle()
        assertFalse(view.keepScreenOn)
    }

    @Test
    fun shortAndLongTextShareOneSize() {
        var text by mutableStateOf(SHORT)
        composeRule.setContent {
            AppTheme {
                ShowContent(
                    text,
                    ShowScheme.BlackOnWhite,
                    onCycleScheme = {},
                    onClose = {},
                    onContinueAsChat = {},
                    textSize = ShowTextSize.Large,
                )
            }
        }
        composeRule.waitForIdle()
        val short = fontSizeOf(SHORT)
        text = LONG
        composeRule.waitForIdle()
        val long = fontSizeOf(LONG)

        assertEquals(ShowTextSize.Large.sp.sp, short)
        assertEquals(short, long)
    }

    @Test
    fun aWordWiderThanTheScreenBreaksAcrossLines() {
        val word = "Supercalifragilisticexpialidocious"
        show(word)

        val layout = layoutOf(word)

        assertTrue(layout.lineCount > 1, "lines: ${layout.lineCount}")
        assertFalse(layout.didOverflowWidth)
    }

    @Test
    fun shortTextHasNoMoreBelowCue() {
        show(SHORT)

        composeRule.onNodeWithText("More below").assertDoesNotExist()
    }

    @Test
    fun moreBelowShowsUntilTheEnd() {
        show(LONG)
        composeRule.onRoot().tryPerformAccessibilityChecks()

        var taps = 0
        while (composeRule.onAllNodesWithText("More below").fetchSemanticsNodes().isNotEmpty()) {
            check(++taps < MAX_PAGES) { "never reached the end" }
            composeRule.onNodeWithText("More below").performClick()
            composeRule.waitForIdle()
        }

        assertTrue(taps > 1, "long text took $taps pages")
        assertEquals(0, closes)
    }

    private fun fontSizeOf(text: String): TextUnit = layoutOf(text).layoutInput.style.fontSize

    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single()
    }

    @Test
    fun eachSizeDrawsTheTextAtItsSizeAndAnnouncesIt() {
        var size by mutableStateOf(ShowTextSize.Small)
        composeRule.setContent {
            AppTheme {
                ShowContent(
                    SHORT,
                    ShowScheme.BlackOnWhite,
                    onCycleScheme = {},
                    onClose = {},
                    onContinueAsChat = {},
                    textSize = size,
                )
            }
        }
        ShowTextSize.entries.forEach { each ->
            size = each
            composeRule.waitForIdle()

            assertEquals(each.sp.sp, fontSizeOf(SHORT))
            composeRule
                .onNodeWithContentDescription("Text size: ${each.name.lowercase()}")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        }
    }

    @Test
    fun theShowScreenTakesTheVolumeKeysOnlyWhileItIsOpen() {
        val keys = VolumeKeys()
        val viewModel = ShowViewModel(FakeSettingsRepository())
        var open by mutableStateOf(true)
        composeRule.setContent {
            CompositionLocalProvider(LocalVolumeKeys provides keys) {
                AppTheme { if (open) ShowScreen(SHORT, emptyList(), viewModel, onClose = {}, onContinueAsChat = {}) }
            }
        }
        composeRule.waitForIdle()

        assertTrue(keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)))
        composeRule.waitForIdle()
        assertEquals(ShowTextSize.Large.sp.sp, fontSizeOf(SHORT))

        open = false
        composeRule.waitForIdle()
        assertFalse(keys.handle(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)))
    }
}
