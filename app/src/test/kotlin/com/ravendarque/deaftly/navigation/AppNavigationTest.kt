package com.ravendarque.deaftly.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.AppContainer
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.theme.AppTheme
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private const val LOAD_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun showApp() {
        composeRule.setContent { AppTheme { AppNavigation(AppContainer(composeRule.activity.applicationContext)) } }
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.home_category_all)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertOnPlaceholder(title: String) {
        composeRule.onNodeWithText(text(R.string.back)).assertExists()
        composeRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun opensEachHomeDestinationAndReturnsWithTheBackButton() {
        composeRule.enableAccessibilityChecks()
        showApp()
        val opens =
            listOf(
                { composeRule.onNodeWithContentDescription(text(R.string.home_new_template)).performClick() } to R.string.route_editor,
                { composeRule.onNodeWithContentDescription(text(R.string.route_settings)).performClick() } to R.string.route_settings,
                { composeRule.onNodeWithText(text(R.string.home_type_and_show)).performClick() } to R.string.route_show,
                { composeRule.onNodeWithText(text(R.string.home_start_chat)).performClick() } to R.string.route_chat,
            )

        opens.forEach { (open, title) ->
            open()
            assertOnPlaceholder(text(title))
            composeRule.onRoot().tryPerformAccessibilityChecks()

            composeRule.onNodeWithText(text(R.string.back)).performClick()
            composeRule.onNodeWithText(text(R.string.home_start_chat)).assertExists()
            composeRule.onNodeWithText(text(R.string.back)).assertDoesNotExist()
        }
    }

    @Test
    fun tappingATemplateOpensFill() {
        showApp()

        composeRule.onAllNodesWithText(text(R.string.template_explain_deaf_title))[0].performClick()

        assertOnPlaceholder(text(R.string.route_fill))
    }

    @Test
    fun systemBackPopsTheStack() {
        showApp()

        composeRule.onNodeWithText(text(R.string.home_start_chat)).performClick()
        composeRule.onNodeWithText(text(R.string.back)).assertExists()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.back)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.home_start_chat)).assertExists()
    }

    @Test
    fun routesRoundTripThroughSerialisation() {
        val routes: List<Route> = listOf(Route.Home, Route.Fill("template-1"), Route.Show, Route.Chat, Route.Editor, Route.Settings)

        routes.forEach { route ->
            val json = Json.encodeToString(Route.serializer(), route)
            assertEquals(route, Json.decodeFromString(Route.serializer(), json))
        }
    }
}
