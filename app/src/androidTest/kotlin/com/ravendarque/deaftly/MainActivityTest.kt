package com.ravendarque.deaftly

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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

    private fun waitForText(value: String) {
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun showsHomeOpensAChatAndEndsItBackToHome() {
        val startChat = text(R.string.home_start_chat)
        val getStarted = text(R.string.first_run_start)
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(startChat).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(getStarted).fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithText(getStarted).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(getStarted).performClick()
            waitForText(startChat)
        }

        composeRule.onNodeWithText(startChat).performClick()
        waitForText(text(R.string.chat_empty))

        composeRule.onNodeWithContentDescription(text(R.string.chat_end)).performClick()
        waitForText(startChat)
        composeRule.onNodeWithText(text(R.string.chat_empty)).assertDoesNotExist()
    }
}
