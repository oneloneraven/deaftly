package com.ravendarque.deaftly.ui.hub

import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

private const val ADD = "Add to home screen"

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB-w390dp-h2000dp")
class HubAddToHomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val added = mutableListOf<Route>()
    private val navigated = mutableListOf<Route>()

    private fun show(canPin: Boolean = true) {
        composeRule.setContent {
            AppTheme {
                HubScreen(onNavigate = { navigated += it }, addToHomeScreen = if (canPin) ({ added += it }) else null)
            }
        }
    }

    private fun openInfo(option: String) = composeRule.onNodeWithContentDescription("About $option").performScrollTo().performClick()

    @Test
    fun eachFeatureWithAShortcutCanBeAddedToTheHomeScreen() {
        show()
        listOf("Start typing", "Face to face", "Side by side", "Listen for me").forEach { option ->
            openInfo(option)
            composeRule.onNodeWithText(ADD).performClick()
            composeRule.onNode(isDialog()).assertDoesNotExist()
        }

        assertEquals(listOf(Route.TypeShow, Route.FaceToFace, Route.Chat(), Route.Listen), added)
        assertEquals(emptyList(), navigated)
    }

    @Test
    fun optionsWithoutAShortcutDoNotOfferIt() {
        show()
        listOf("Use a template", "Help", "Share").forEach { option ->
            openInfo(option)
            composeRule.onNodeWithText(ADD).assertDoesNotExist()
            composeRule.onNodeWithText("Close").performClick()
        }
    }

    @Test
    fun launchersThatCannotPinDoNotOfferIt() {
        show(canPin = false)
        openInfo("Face to face")

        composeRule.onNodeWithText(ADD).assertDoesNotExist()
    }
}
