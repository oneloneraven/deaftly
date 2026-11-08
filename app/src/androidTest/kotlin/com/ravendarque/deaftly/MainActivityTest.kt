package com.ravendarque.deaftly

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val LOAD_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    @Test
    fun showsHomeAndNavigatesToAPlaceholderAndBack() {
        val title = text(R.string.home_start_chat)
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(text(R.string.first_run_start)).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText(text(R.string.first_run_start)).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(text(R.string.first_run_start)).performClick()
        }
        composeRule.onNodeWithText(title).assertExists()

        composeRule.onNodeWithText(title).performClick()
        composeRule.onNodeWithText(text(R.string.back)).assertExists()
        composeRule.onNodeWithText(text(R.string.route_chat)).assertExists()

        composeRule.onNodeWithText(text(R.string.back)).performClick()
        composeRule.onNodeWithText(title).assertExists()
        composeRule.onNodeWithText(text(R.string.back)).assertDoesNotExist()
    }
}
