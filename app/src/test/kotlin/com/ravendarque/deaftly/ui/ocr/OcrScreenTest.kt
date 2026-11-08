package com.ravendarque.deaftly.ui.ocr

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class OcrScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var closes = 0

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show() {
        composeRule.setContent {
            AppTheme {
                OcrCameraContent(
                    status = "Live",
                    phrases = menuPhrases,
                    onClose = { closes++ },
                    controlsEnabled = false,
                    viewfinder = { FakeCamera(it) },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun showsTheCameraViewTheStatusAndTheControls() {
        show()

        composeRule.onNodeWithContentDescription("Camera view").assertIsDisplayed()
        composeRule.onNodeWithText("Live").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close camera").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Torch").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Freeze camera").assertIsNotEnabled()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theCloseButtonLeaves() {
        show()

        composeRule.onNodeWithContentDescription("Close camera").performClick()

        assertEquals(1, closes)
    }
}
