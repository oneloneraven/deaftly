package com.ravendarque.deaftly.ui.help

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.help.HelpRepository
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private val titlesInOrder =
    listOf(
        "Quick start",
        "Installing and updating",
        "Templates",
        "Show and Type & show",
        "Face to face",
        "Side by side",
        "Reading text with the camera",
        "Settings",
        "Troubleshooting",
    )

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class HelpScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val repository = HelpRepository(ApplicationProvider.getApplicationContext<Context>().assets)
    private val opened = mutableListOf<String>()
    private val externalLinks = mutableListOf<String>()
    private val external =
        object : UriHandler {
            override fun openUri(uri: String) {
                externalLinks += uri
            }
        }

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
    }

    private fun isHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun showTopics() {
        composeRule.setContent {
            AppTheme { HelpTopicsScreen(repository, onOpen = { opened += it }, onBack = {}) }
        }
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) { composeRule.onAllNodesWithText("Quick start").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun showPage(
        slug: String,
        waitFor: String,
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides external) {
                AppTheme { HelpPageScreen(slug, repository, onOpenTopic = { opened += it }, onBack = onBack) }
            }
        }
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) { composeRule.onAllNodesWithText(waitFor).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun theTopicsAreListedInOrderUnderAHelpHeading() {
        showTopics()

        composeRule.onNode(hasText("Help").and(isHeading())).assertExists()
        val tops =
            titlesInOrder.map {
                composeRule
                    .onNodeWithText(it)
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun everyTopicIsAFullSizeButton() {
        showTopics()

        titlesInOrder.forEach {
            composeRule.onNodeWithText(it).assertHasClickAction().assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun tappingATopicOpensItsPage() {
        showTopics()

        composeRule.onNodeWithText("Templates").performClick()

        assertEquals(listOf("templates"), opened)
    }

    @Test
    fun theTopicsPassAccessibilityChecks() {
        showTopics()

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aPageShowsItsContentWithHeadingsAsHeadings() {
        showPage("templates", waitFor = "Using a template")

        composeRule.onNode(hasText("Templates").and(isHeading())).assertExists()
        composeRule.onNode(hasText("Using a template").and(isHeading())).assertExists()
    }

    @Test
    fun aPagePassesAccessibilityChecks() {
        showPage("templates", waitFor = "Using a template")

        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun backCallsBack() {
        var backs = 0
        showPage("templates", waitFor = "Using a template", onBack = { backs++ })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    private fun clickLink(
        containing: String,
        linkText: String,
    ) {
        val node = composeRule.onNode(hasText(containing, substring = true))
        val config = node.fetchSemanticsNode().config
        val offset = config[SemanticsProperties.Text].joinToString("") { it.text }.indexOf(linkText)
        val layouts = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val target = layouts.first().getBoundingBox(offset + 1).center
        node.performTouchInput { click(target) }
    }

    @Test
    fun anInternalLinkOpensThatPage() {
        showPage("templates", waitFor = "Using a template")

        clickLink("See Reading text", "Reading text with the camera")

        assertEquals(listOf("camera"), opened)
        assertEquals(emptyList(), externalLinks)
    }

    @Test
    fun anHttpsLinkGoesToTheSystemUriHandler() {
        showPage("installing-and-updating", waitFor = "Installing and updating")

        clickLink("open the download page", "download page")

        assertEquals(listOf("https://deaftly.com/download/"), externalLinks)
        assertEquals(emptyList(), opened)
    }
}

private const val LOAD_TIMEOUT_MILLIS = 10_000L
