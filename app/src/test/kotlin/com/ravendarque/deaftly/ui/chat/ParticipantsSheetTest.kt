package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
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
@Config(qualifiers = "w390dp-h1600dp")
class ParticipantsSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var conversation by mutableStateOf(Conversation(Participant.defaultA(), Participant.defaultB()))
    private var done = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(initial: Conversation = conversation) {
        conversation = initial
        composeRule.setContent {
            AppTheme {
                Surface {
                    Column {
                        ParticipantsSheetContent(
                            conversation = conversation,
                            onChange = { conversation = conversation.withParticipant(it) },
                            onDone = { done++ },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun swatches(name: String) = composeRule.onAllNodesWithContentDescription(name)

    private fun hasState(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun offersAnOptionalNameAndEveryColourToBothPeopleAndSaysWhatBlankMeans() {
        show()

        composeRule.onAllNodesWithText("Name (optional)").assertCountEquals(2)
        composeRule.onNodeWithText("Shown as \u201cMe\u201d if left blank").assertExists()
        composeRule.onNodeWithText("Shown as \u201cThem\u201d if left blank").assertExists()
        listOf("Violet", "Peach", "Teal", "Rose", "Forest").forEach { swatches(it).assertCountEquals(2) }
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun typingANameSetsThatPersonsName() {
        show()

        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("Sam")

        assertEquals("Sam", conversation.participantB.name)
        assertEquals(null, conversation.participantA.name)
    }

    @Test
    fun clearingANameReturnsToTheDefault() {
        show(Conversation(Participant.defaultA("Alex"), Participant.defaultB()))

        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("")

        assertEquals(null, conversation.participantA.name)
    }

    @Test
    fun choosingAColourSelectsItForThatPersonOnly() {
        show()

        swatches("Teal")[0].performClick()

        assertEquals(ColourRole.Teal, conversation.participantA.colour)
        assertEquals(ColourRole.PersonB, conversation.participantB.colour)
        swatches("Teal")[0].assertIsSelected()
        swatches("Teal")[1].assertIsNotSelected()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theColourTheOtherPersonHasIsUnavailableAndSaysWhoUsesIt() {
        show(Conversation(Participant.defaultA("Alex"), Participant.defaultB("Sam")))

        swatches("Peach")[0].assertIsNotEnabled().assert(hasState("Used by Sam"))
        swatches("Violet")[1].assertIsNotEnabled().assert(hasState("Used by Alex"))
        swatches("Teal")[0].assertIsEnabled()
    }

    @Test
    fun theChosenColourIsNamedInTextAsWellAsShown() {
        show(Conversation(Participant(ParticipantId.A, ColourRole.Rose), Participant.defaultB()))

        composeRule.onNodeWithText("Colour: Rose").assertExists()
        composeRule.onNodeWithText("Colour: Peach").assertExists()
    }

    @Test
    fun doneIsReported() {
        show()

        composeRule.onNodeWithText("Done").performClick()

        assertEquals(1, done)
    }

    @Test
    fun aLongNameIsCutToTheLimit() {
        show()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("x".repeat(PARTICIPANT_NAME_MAX_LENGTH + 10))

        assertEquals(PARTICIPANT_NAME_MAX_LENGTH, conversation.participantA.name?.length)
    }
}
