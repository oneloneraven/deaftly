package com.ravendarque.deaftly

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.navigation.Route
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    @Test
    fun showsHomeAndNavigatesToAPlaceholderAndBack() {
        val title = text(Route.Chat.title)
        composeRule.onNodeWithText(title).assertExists()

        composeRule.onNodeWithText(title).performClick()
        composeRule.onNodeWithText(text(R.string.back)).assertExists()

        composeRule.onNodeWithText(text(R.string.back)).performClick()
        composeRule.onNodeWithText(title).assertExists()
        composeRule.onNodeWithText(text(R.string.back)).assertDoesNotExist()
    }
}
