package com.ravendarque.deaftly.ui.show

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.settings.ShowScheme
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
private val LONG = List(150) { "Could I have the pasta arrabbiata please? I am deaf." }.joinToString(" ")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
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
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) {
                ShowContent(
                    text,
                    scheme,
                    onCycleScheme = { scheme = scheme.next() },
                    onClose = { closes++ },
                    onContinueAsChat = { chats++ },
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
    fun fitsShortTextLargeAndLongTextAtTheMinimum() {
        var short = 0
        var long = 0
        var wide = 0
        composeRule.setContent {
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val area = Constraints(maxWidth = 1000, maxHeight = 1400)
            short = fitFontSizeSp(SHORT, area, density, measurer)
            long = fitFontSizeSp(LONG, area, density, measurer)
            wide = fitFontSizeSp("Supercalifragilisticexpialidocious", area, Density(1f), measurer)
        }
        composeRule.waitForIdle()

        assertTrue(short > MIN_SHOW_FONT_SP, "short text fills the space: $short")
        assertEquals(MIN_SHOW_FONT_SP, long)
        assertTrue(wide in MIN_SHOW_FONT_SP..MAX_SHOW_FONT_SP)
    }
}
