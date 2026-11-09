package com.ravendarque.deaftly.ui.ocr

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.ocr.OcrSelection
import com.ravendarque.deaftly.core.ocr.PhraseSelection
import com.ravendarque.deaftly.core.ocr.TextSelection
import com.ravendarque.deaftly.core.ocr.WordRef
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
    private val ranges = mutableListOf<TextSelection>()
    private var selection by mutableStateOf(OcrSelection())
    private var fieldText by mutableStateOf("")
    private var label by mutableStateOf<String?>(null)
    private var frozen by mutableStateOf(false)
    private var torch by mutableStateOf<Boolean?>(null)
    private var freezes = 0
    private val added = mutableListOf<String>()
    private var torches = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show() {
        composeRule.setContent {
            AppTheme {
                OcrCameraContent(
                    content = OcrContent("Live", menuPhrases, selection, fieldText, frozen = frozen, torch = torch, fieldLabel = label),
                    actions =
                        OcrActions(
                            onClose = { closes++ },
                            onSelectRange = { range ->
                                ranges += range
                                selection = selection.dragged(range)
                                fieldText = PhraseSelection.text(menuPhrases, range).orEmpty()
                            },
                            onTapPhrase = { id ->
                                taps += id
                                val phrase = menuPhrases.firstOrNull { it.id == id }
                                selection = if (phrase == null) OcrSelection() else selection.toggled(phrase)
                                if (phrase != null) fieldText = selection.tappedText
                            },
                            onFieldChange = { fieldText = it },
                            onClearText = {
                                selection = OcrSelection()
                                fieldText = ""
                            },
                            onToggleFreeze = {
                                freezes++
                                frozen = !frozen
                            },
                            onAdd = { added += fieldText.trim() },
                            onToggleTorch = {
                                torches++
                                torch = torch?.not()
                            },
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
        composeRule.onNodeWithContentDescription("Torch").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Freeze camera").assertIsOff()
        composeRule.onNodeWithText("Tap lines, or drag across words.").assertIsDisplayed()
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
    fun tappingSeveralLinesSelectsEachAndJoinsTheirTextInTapOrder() {
        show()

        composeRule.onNodeWithContentDescription("Garlic bread 4.50").performClick()
        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Garlic bread 4.50").assertIsSelected()
        composeRule.onNodeWithContentDescription(lasagne).assertIsSelected()
        composeRule.onNodeWithContentDescription("Pasta arrabbiata 12.50").assertIsNotSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains("Garlic bread 4.50 $lasagne")
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun activatingASelectedLineNodeAgainDeselectsItAndRemovesItsText() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").performClick()

        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(lasagne).assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").assertIsSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains("Garlic bread 4.50")
        assertEquals("Garlic bread 4.50", fieldText)
    }

    @Test
    fun tappingLinesOnTheCameraViewTogglesThemToo() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            click(Offset(width * 0.5f, height * 0.452f))
            click(Offset(width * 0.5f, height * 0.562f))
        }
        composeRule.waitForIdle()

        assertEquals("$lasagne Garlic bread 4.50", fieldText)
    }

    @Test
    fun aDragAfterTappingLinesReplacesTheTappedSelection() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").performClick()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            down(centre(1, 1, width, height))
            moveTo(centre(3, 0, width, height))
            up()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(lasagne).assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").assertIsNotSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains("arrabbiata 12.50 tomato, garlic, chilli Spaghetti")
    }

    @Test
    fun theClearButtonIsHiddenWhileTheFieldIsEmpty() {
        show()

        composeRule.onNodeWithContentDescription("Clear text").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Clear text").assertIsDisplayed()
    }

    @Test
    fun theClearButtonHasAnAccessibleLabelAndAFortyEightDpTarget() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.waitForIdle()

        composeRule
            .onNode(hasContentDescription("Clear text") and hasClickAction())
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theClearButtonEmptiesTheFieldAndDeselectsEveryLine() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").performClick()

        composeRule.onNodeWithContentDescription("Clear text").performClick()
        composeRule.waitForIdle()

        assertEquals("", fieldText)
        composeRule.onNodeWithContentDescription(lasagne).assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Garlic bread 4.50").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Clear text").assertDoesNotExist()
        composeRule.onNodeWithText("Add to message").assertIsNotEnabled()
    }

    @Test
    fun theClearButtonAlsoShowsForTypedText() {
        show()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Hello")
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Clear text").performClick()
        composeRule.waitForIdle()

        assertEquals("", fieldText)
    }

    @Test
    fun theTrayTextCanBeEdited() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Lasagne verde")

        assertEquals("Lasagne verde", fieldText)
    }

    private fun centre(
        line: Int,
        word: Int,
        width: Int,
        height: Int,
    ): Offset {
        val box = menuPhrases[line].words[word].quad.boundingBox()
        return Offset((box.left + box.right) / 2 * width, (box.top + box.bottom) / 2 * height)
    }

    @Test
    fun draggingFromOneWordToAnotherOnALaterLineSelectsTheRangeInReadingOrder() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            down(centre(1, 1, width, height))
            moveTo(centre(1, 2, width, height))
            moveTo(centre(3, 0, width, height))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(TextSelection(WordRef(2, 1), WordRef(4, 0)), ranges.last())
        composeRule
            .onNode(hasSetTextAction())
            .assertTextContains("arrabbiata 12.50 tomato, garlic, chilli Spaghetti")
    }

    @Test
    fun draggingBackwardsSelectsTheSameWords() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            down(centre(3, 0, width, height))
            moveTo(centre(1, 1, width, height))
            up()
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).assertTextContains("arrabbiata 12.50 tomato, garlic, chilli Spaghetti")
    }

    @Test
    fun aDragThatStartsFarFromAnyWordSelectsNothing() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            down(Offset(width * 0.5f, height * 0.06f))
            moveTo(centre(1, 1, width, height))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(emptyList(), ranges)
    }

    @Test
    fun draggingTheEndHandleMovesOnlyThatEnd() {
        show()
        composeRule.onNodeWithContentDescription("Pasta arrabbiata 12.50").performClick()
        composeRule.waitForIdle()
        ranges.clear()

        composeRule.onNodeWithContentDescription("Camera view").performTouchInput {
            val end =
                menuPhrases[1]
                    .words
                    .last()
                    .quad.points[2]
            down(Offset(end.x * width, end.y * height))
            moveTo(centre(3, 0, width, height))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(TextSelection(WordRef(2, 0), WordRef(4, 0)), ranges.last())
    }

    @Test
    fun theFreezeButtonIsAToggleThatReportsItsState() {
        show()

        composeRule.onNodeWithContentDescription("Freeze camera").assertIsOff().performClick()
        composeRule.waitForIdle()

        assertEquals(1, freezes)
        composeRule.onNodeWithContentDescription("Freeze camera").assertIsOn()
        composeRule.onNodeWithContentDescription("Freeze camera").assert(hasStateDescription("Frozen"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theTorchButtonIsHiddenUnlessTheDeviceHasAFlash() {
        show()
        composeRule.onNodeWithContentDescription("Torch").assertDoesNotExist()

        torch = false
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Torch")
            .assertIsOff()
            .assert(hasStateDescription("Off"))
            .performClick()
        composeRule.waitForIdle()
        assertEquals(1, torches)
        composeRule.onNodeWithContentDescription("Torch").assertIsOn().assert(hasStateDescription("On"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    private fun hasStateDescription(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun addToMessageIsDisabledUntilThereIsText() {
        show()

        composeRule.onNodeWithText("Add to message").assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("   ")
        composeRule.onNodeWithText("Add to message").assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Add to message").assertIsEnabled()
    }

    @Test
    fun addToMessageSendsTheEditedTextWithoutSurroundingSpaces() {
        show()
        composeRule.onNodeWithContentDescription(lasagne).performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("  Lasagne verde ")

        composeRule.onNodeWithText("Add to message").performClick()

        assertEquals(listOf("Lasagne verde"), added)
    }

    @Test
    fun theFieldIsLabelledWithThePlaceholdersLabel() {
        label = "Menu item"
        show()

        composeRule.onNode(hasSetTextAction()).assert(hasText("Menu item"))
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
