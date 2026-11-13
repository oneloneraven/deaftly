package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class BubbleTailTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val tail = BubbleTailState()
    private lateinit var viewModel: ChatViewModel

    private fun show(swapped: Boolean = false) {
        val saved = SavedStateHandle(mapOf("conversation" to ChatCodec.encode(scriptConversation(turn = ParticipantId.A))))
        viewModel = ChatViewModel(FakeSettingsRepository(sidesSwapped = swapped), saved)
        composeRule.setContent {
            AppTheme {
                val current by viewModel.state.collectAsState()
                val state = current as? ChatUiState.Ready ?: return@AppTheme
                Column {
                    Composer(
                        participant = state.conversation.activeParticipant,
                        hasSent = true,
                        draft = state.draft,
                        textSize = state.textSize,
                        actions = ComposerActions({}, {}, {}, {}),
                        tail = tail,
                    )
                    TurnControl(state.conversation, state.sidesSwapped, onSelect = viewModel::selectTurn, tail = tail)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun nameCentreX(name: String) =
        composeRule
            .onNodeWithText(name, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot.center.x

    @Test
    fun theTailTargetsTheActiveSpeakersName() {
        show()

        assertNotNull(tail.name)
        assertTrue(kotlin.math.abs(tail.name!!.center.x - nameCentreX("Sam")) < 1f)
    }

    @Test
    fun theTailMovesToTheOtherNameWhenTheTurnChanges() {
        show()
        val before = tail.name!!.center.x

        composeRule.runOnUiThread { viewModel.selectTurn(ParticipantId.B) }
        composeRule.waitForIdle()

        assertTrue(tail.name!!.center.x < before)
        assertTrue(kotlin.math.abs(tail.name!!.center.x - nameCentreX("Waiter")) < 1f)
    }

    @Test
    fun theTailFollowsTheNamesWhenSidesAreSwapped() {
        show(swapped = true)

        assertTrue(kotlin.math.abs(tail.name!!.center.x - nameCentreX("Sam")) < 1f)
        assertTrue(tail.name!!.center.x < 390f * 2)
        assertEquals(true, nameCentreX("Sam") < nameCentreX("Waiter"))
    }

    @Test
    fun theTipSitsOverTheLetterNearestTheSwitch() {
        show()
        val sam = tail.name!!
        assertTrue(tail.tipX!! - sam.left < sam.width / 3)

        composeRule.runOnUiThread { viewModel.selectTurn(ParticipantId.B) }
        composeRule.waitForIdle()
        val waiter = tail.name!!
        assertTrue(waiter.right - tail.tipX!! < waiter.width / 6)
    }

    @Test
    fun theTipStaysOverTheInnerLetterWhenSidesAreSwapped() {
        show(swapped = true)
        val sam = tail.name!!

        assertTrue(sam.right - tail.tipX!! < sam.width / 3)
    }

    @Test
    fun bothNamesStayFortyEightDpTargets() {
        show()

        listOf("Sam", "Waiter").forEach {
            composeRule.onNodeWithText(it).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }
}
