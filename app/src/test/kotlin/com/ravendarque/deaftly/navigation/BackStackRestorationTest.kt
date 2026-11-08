package com.ravendarque.deaftly.navigation

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.MainActivity
import com.ravendarque.deaftly.R
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

    @Test
    fun theBackStackSurvivesRecreation() {
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.home_start_chat)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text(R.string.home_start_chat)).performClick()
        composeRule.onNodeWithText(text(R.string.route_chat)).assertExists()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText(text(R.string.route_chat)).assertExists()
        composeRule.onNodeWithText(text(R.string.back)).assertExists()
    }
}
