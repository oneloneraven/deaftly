package com.ravendarque.deaftly.ui.ocr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.ocr.PhraseSelection
import com.ravendarque.deaftly.core.ocr.TextSelection
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class OcrScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var closes = 0
    private val taps = mutableListOf<Long?>()
    private var selection by mutableStateOf<TextSelection?>(null)
    private var fieldText by mutableStateOf("")

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show() {
        composeRule.setContent {
            AppTheme {
                OcrCameraContent(
                    content = OcrContent("Live", menuPhrases, selection, fieldText, controlsEnabled = false),
                    actions =
                        OcrActions(
                            onClose = { closes++ },
                            onTapPhrase = { id ->
                                taps += id
                                val phrase = menuPhrases.firstOrNull { it.id == id }
                                selection = phrase?.let(PhraseSelection::wholePhrase)
                                if (phrase != null) fieldText = phrase.text
                            },
                            onFieldChange = { fieldText = it },
                        ),
                    viewfinder = { FakeCamera(it) },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private val lasagne = "Lasagne 14.00"

    @Test
    fun showsTheCameraViewTheStatusTheControlsAndTheTray() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").assertIsDisplayed()
        composeRule.onNodeWithText("Live").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close camera").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Torch").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Freeze camera").assertIsNotEnabled()
        composeRule.onNodeWithText("Tap a phrase to pick it.").assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theCloseButtonLeaves() {
        show()

        composeRule.onNodeWithContentDescription("Close camera").performClick()

        assertEquals(1, closes)
    }

    @Test
    fun everyDetectedPhraseIsAFocusableNodeLabelledWithItsTextAndCanBeClicked() {
        show()

        menuPhrases.forEach { phrase ->
            composeRule.onNodeWithContentDescription(phrase.text).assert(hasClickAction())
        }
    }

    @Test
    fun activatingAPhraseNodeSelectsItAndPutsItsTextInTheTray() {
        show()

        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<Long?>(6), taps)
        composeRule.onNodeWithContentDescription(lasagne).assertIsSelected()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").assertIsNotSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains(lasagne)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun tappingAPhraseOnTheCameraViewSelectsIt() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            click(Offset(width * 0.5f, height * 0.452f))
        }
        composeRule.waitForIdle()

        assertEquals(listOf<Long?>(6), taps)
        composeRule.onNodeWithContentDescription(lasagne).assertIsSelected()
    }

    @Test
    fun tappingEmptySpaceClearsTheSelectionButKeepsTheText() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            click(Offset(width * 0.5f, height * 0.08f))
        }
        composeRule.waitForIdle()

        assertEquals(listOf<Long?>(6, null), taps)
        composeRule.onNodeWithContentDescription(lasagne).assertIsNotSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains(lasagne)
    }

    @Test
    fun theTrayTextCanBeEdited() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Lasagne verde")

        assertEquals("Lasagne verde", fieldText)
    }
}
