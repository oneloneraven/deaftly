package com.ravendarque.deaftly.navigation

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.MainActivity
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.chat.isParticipantNameField
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LOAD_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class BackStackRestorationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun text(
        id: Int,
        vararg args: Any,
    ) = composeRule.activity.getString(id, *args)

    private fun getStarted() {
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.first_run_start)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text(R.string.first_run_start)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.home_start_chat)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theWelcomeSurvivesRecreationUntilGetStartedIsPressed() {
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.first_run_start)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText(text(R.string.first_run_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.home_start_chat)).assertDoesNotExist()
    }

    @Test
    fun theBackStackSurvivesRecreation() {
        getStarted()
        composeRule.onNodeWithContentDescription(text(R.string.route_settings)).performClick()
        composeRule.onNodeWithText(text(R.string.route_settings)).assertExists()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText(text(R.string.settings_layout_heading)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.back)).assertExists()
    }

    @Test
    fun theChatParticipantsSurviveRecreation() {
        getStarted()
        composeRule.onNodeWithText(text(R.string.home_start_chat)).performClick()
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.chat_empty)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(text(R.string.chat_edit_participants, "Them", "Me")).performClick()
        composeRule.onAllNodes(isParticipantNameField)[1].performTextInput("Sam")
        composeRule.onNodeWithText(text(R.string.participants_done)).performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithContentDescription(text(R.string.chat_edit_participants, "Sam", "Me")).assertExists()
    }
}
