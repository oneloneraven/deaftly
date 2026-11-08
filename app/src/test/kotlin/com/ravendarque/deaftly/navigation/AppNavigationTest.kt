package com.ravendarque.deaftly.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.AppContainer
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    @Test
    fun opensEachDestinationFromHomeAndReturnsWithTheBackButton() {
        composeRule.enableAccessibilityChecks()
        composeRule.setContent { AppTheme { AppNavigation(AppContainer(composeRule.activity.applicationContext)) } }

        Route.entries.filter { it != Route.Home }.forEach { route ->
            val title = text(route.title)
            composeRule.onNodeWithText(title).performClick()
            composeRule.onNodeWithText(text(R.string.back)).assertExists()
            composeRule.onRoot().tryPerformAccessibilityChecks()

            composeRule.onNodeWithText(text(R.string.back)).performClick()
            composeRule.onNodeWithText(title).assertExists()
            composeRule.onNodeWithText(text(R.string.back)).assertDoesNotExist()
        }
    }

    @Test
    fun systemBackPopsTheStack() {
        composeRule.setContent { AppTheme { AppNavigation(AppContainer(composeRule.activity.applicationContext)) } }
        val title = text(Route.Chat.title)

        composeRule.onNodeWithText(title).performClick()
        composeRule.onNodeWithText(text(R.string.back)).assertExists()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.back)).assertDoesNotExist()
        composeRule.onNodeWithText(title).assertExists()
    }
}
