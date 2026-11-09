package com.ravendarque.deaftly.ui.hub

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private val options = listOf("Start typing", "Use a template", "Face to face", "Side by side")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h1600dp")
class HubScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val navigated = mutableListOf<Route>()

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        composeRule.setContent { AppTheme { HubScreen(onNavigate = { navigated += it }) } }
        composeRule.waitForIdle()
    }

    private fun isHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun showsTheWordmarkAndSettings() {
        composeRule.onNodeWithText("deaftly").assert(hasContentDescription("deftly"))
        composeRule.onNodeWithContentDescription("Settings").assertHasClickAction()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theGroupHeadingsAreHeadings() {
        composeRule.onNodeWithText("Show").assert(isHeading())
        composeRule.onNodeWithText("Chat").assert(isHeading())
    }

    @Test
    fun everyOptionIsALabelledButtonWithAFullTouchTarget() {
        options.forEach {
            composeRule
                .onNodeWithText(it)
                .assertHasClickAction()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun eachOptionOpensItsRoute() {
        options.forEach { composeRule.onNodeWithText(it).performClick() }
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(
            listOf(Route.TypeShow, Route.Templates, Route.FaceToFace, Route.Chat(), Route.Settings),
            navigated,
        )
    }
}
