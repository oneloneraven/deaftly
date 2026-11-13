package com.ravendarque.deaftly.ui.share

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h1600dp")
class ShareScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val shared = mutableListOf<String>()
    private var backs = 0

    @Before
    fun setUp() {
        composeRule.enableAccessibilityChecks()
        composeRule.setContent { AppTheme { ShareScreen(onShare = { shared += it }, onBack = { backs++ }) } }
        composeRule.waitForIdle()
    }

    @Test
    fun showsTheTitleTheQrCodeAndTheUrl() {
        composeRule.onNodeWithText("Share Deaftly").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("QR code for deaftly.com/download").assertIsDisplayed()
        composeRule.onNodeWithText("deaftly.com/download").assertIsDisplayed()
    }

    @Test
    fun theQrCodeIsSquareAndLargeButCapped() {
        val bounds = composeRule.onNodeWithContentDescription("QR code for deaftly.com/download").fetchSemanticsNode().boundsInRoot
        assertEquals(bounds.width, bounds.height, 1f)
        assertTrue(bounds.width > 250f)
    }

    @Test
    fun theQrCodeComesBeforeTheUrlAndTheButton() {
        val qr = composeRule.onNodeWithContentDescription("QR code for deaftly.com/download").fetchSemanticsNode().boundsInRoot
        val url = composeRule.onNodeWithText("deaftly.com/download").fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithText("Share link").fetchSemanticsNode().boundsInRoot
        assertTrue(qr.bottom <= url.top)
        assertTrue(url.bottom <= button.top)
    }

    @Test
    fun theShareButtonPassesTheDownloadUrl() {
        composeRule
            .onNodeWithText("Share link")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(listOf("https://deaftly.com/download/"), shared)
    }

    @Test
    fun backNavigatesBack() {
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun passesAccessibilityChecks() {
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
