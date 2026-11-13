package com.ravendarque.deaftly.ui.hub

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onLast
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
import kotlin.test.assertTrue

private val options = listOf("Start typing", "Use a template", "Face to face", "Side by side", "Help", "Share")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
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
        composeRule.onNodeWithText("Support").assert(isHeading())
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

    private fun cardHeights() =
        options.map {
            composeRule
                .onNodeWithText(it)
                .fetchSemanticsNode()
                .boundsInRoot.height
        }

    @Test
    fun shareSitsNextToHelpOnTheSameRow() {
        val help = composeRule.onNodeWithText("Help").fetchSemanticsNodeBounds()
        val share = composeRule.onNodeWithText("Share").fetchSemanticsNodeBounds()
        assertEquals(help.top, share.top, 0.5f)
        assertTrue(share.left > help.right)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNodeBounds() = fetchSemanticsNode().boundsInRoot

    @Test
    @Config(fontScale = 2f)
    fun shareStacksBelowHelpAtLargeFontScale() {
        val help = composeRule.onNodeWithText("Help").fetchSemanticsNodeBounds()
        val share = composeRule.onNodeWithText("Share").fetchSemanticsNodeBounds()
        assertTrue(share.top > help.bottom)
    }

    @Test
    fun allCardsAreTheSameHeightAcrossAllGroups() {
        assertEquals(1, cardHeights().distinct().size)
    }

    @Test
    @Config(fontScale = 2f)
    fun allCardsAreTheSameHeightAtLargeFontScale() {
        assertEquals(1, cardHeights().distinct().size)
    }

    @Test
    @Config(fontScale = 1.4f)
    fun allCardsAreTheSameHeightWhenOnlyTheLongerTitlesWrap() {
        assertEquals(1, cardHeights().distinct().size)
    }

    @Test
    fun eachOptionOpensItsRoute() {
        options.forEach { composeRule.onNodeWithText(it).performClick() }
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(
            listOf(Route.TypeShow, Route.Templates, Route.FaceToFace, Route.Chat(), Route.Help, Route.Share, Route.Settings),
            navigated,
        )
    }

    private val help =
        mapOf(
            "Start typing" to
                "Type what you want to say, then show it full screen in large, clear text for the other person to read. " +
                "Use this for anything you haven’t saved as a template.",
            "Use a template" to
                "Templates are messages you use often, such as ordering a coffee or giving your name at reception. " +
                "Pick one, fill in the parts that change each time, like a time or a name, then show it. " +
                "You can type the details or read them from a sign or document with the camera.",
            "Face to face" to
                "For typing to someone opposite you while securely holding your phone. The keyboard faces you, " +
                "while the text you type faces away from you so they can read along as you type. " +
                "If they want to reply, you can switch to Side by side and pass them the phone.",
            "Side by side" to
                "For a back-and-forth conversation. You share one phone, passing it between you and taking turns to type. " +
                "Each person’s messages appear in their own colour, like a script, so it’s easy to follow who said what. " +
                "Flip the switch to change whose turn it is.",
            "Help" to "Find out how to use Deaftly: templates, Show, Face to face, Side by side and the camera.",
            "Share" to "Show someone the QR code so they can get Deaftly, or send them the link.",
        )

    private fun info(option: String) = composeRule.onNodeWithContentDescription("About $option")

    @Test
    fun everyCardHasASmallInfoButtonWithAFullTouchTargetAndNoDescription() {
        options.forEach {
            info(it).assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithText("Enter your own custom text").assertDoesNotExist()
    }

    @Test
    fun theInfoButtonOpensTheOptionsHelpAndDoesNotNavigate() {
        options.forEach { option ->
            info(option).performClick()

            composeRule.onNode(isDialog()).assertExists()
            composeRule.onNodeWithText(help.getValue(option)).assertIsDisplayed()
            composeRule.onNode(hasText(option).and(isHeading())).assertExists()
            composeRule.onNodeWithText("Close").performClick()
            composeRule.onNode(isDialog()).assertDoesNotExist()
        }
        assertEquals(emptyList(), navigated)
    }

    @Test
    fun theDialogPassesAccessibilityChecks() {
        info("Face to face").performClick()

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onAllNodes(isRoot()).onLast().tryPerformAccessibilityChecks()
        composeRule.onNodeWithText("Start").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Close").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun closeAndStartShareAVerticalCentre() {
        info("Face to face").performClick()

        val start = composeRule.onNodeWithText("Start").fetchSemanticsNode().boundsInRoot
        val close = composeRule.onNodeWithText("Close").fetchSemanticsNode().boundsInRoot
        assertEquals(start.center.y, close.center.y, 0.5f)
    }

    @Test
    fun startClosesTheDialogAndOpensTheOptionsRoute() {
        info("Face to face").performClick()

        composeRule.onNodeWithText("Start").performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(listOf<Route>(Route.FaceToFace), navigated)
    }

    @Test
    fun tappingACardStillNavigatesAndShowsNoDialog() {
        composeRule.onNodeWithText("Use a template").performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(listOf<Route>(Route.Templates), navigated)
    }
}
