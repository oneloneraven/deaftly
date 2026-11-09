package com.ravendarque.deaftly.ui.ocr

import androidx.compose.material3.Text
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
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
class CameraGateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    @Before
    fun enableChecks() {
        composeRule.enableAccessibilityChecks()
    }

    private fun show(
        access: CameraAccess,
        unavailable: Boolean = false,
    ) {
        composeRule.setContent {
            AppTheme {
                CameraGate(
                    access = access,
                    unavailable = unavailable,
                    actions =
                        CameraGateActions(
                            onAllow = { calls += "allow" },
                            onOpenSettings = { calls += "settings" },
                            onTypeInstead = { calls += "type" },
                            onClose = { calls += "close" },
                        ),
                ) {
                    Text("camera")
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun explainsWhyTheCameraIsNeededBeforeAsking() {
        show(CameraAccess.NotAsked)

        composeRule.onNodeWithText("Deaftly uses the camera to read text. Pictures never leave your phone.").assertIsDisplayed()
        composeRule.onNodeWithText("camera").assertDoesNotExist()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theAppNameInTheExplanationIsSpokenAsDeftly() {
        show(CameraAccess.NotAsked)

        composeRule
            .onNodeWithText("Deaftly uses the camera to read text. Pictures never leave your phone.")
            .assert(hasContentDescription("Deftly uses the camera to read text. Pictures never leave your phone."))
    }

    @Test
    fun allowingFromTheExplanationAsksForThePermission() {
        show(CameraAccess.NotAsked)

        composeRule.onNodeWithText("Allow camera").performClick()

        assertEquals(listOf("allow"), calls)
    }

    @Test
    fun typingInsteadFromTheExplanationLeavesWithoutAsking() {
        show(CameraAccess.NotAsked)

        composeRule.onNodeWithText("Type it instead").performClick()

        assertEquals(listOf("type"), calls)
    }

    @Test
    fun aDeniedPermissionOffersTypingAndAskingAgain() {
        show(CameraAccess.Denied)

        composeRule.onNodeWithText("The camera is off").assertIsDisplayed()
        composeRule.onNodeWithText("Type it instead").assertIsDisplayed()
        composeRule.onNodeWithText("Open settings").assertDoesNotExist()
        composeRule.onNodeWithText("Allow camera").performClick()
        assertEquals(listOf("allow"), calls)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aPermanentlyDeniedPermissionOffersSettingsInsteadOfAskingAgain() {
        show(CameraAccess.DeniedForGood)

        composeRule.onNodeWithText("Allow camera").assertDoesNotExist()
        composeRule.onNodeWithText("Open settings").performClick()
        composeRule.onNodeWithText("Type it instead").performClick()

        assertEquals(listOf("settings", "type"), calls)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aGrantedPermissionShowsTheCamera() {
        show(CameraAccess.Granted)

        composeRule.onNodeWithText("camera").assertIsDisplayed()
        composeRule.onNodeWithText("Allow camera").assertDoesNotExist()
    }

    @Test
    fun aPhoneWithoutACameraOffersTyping() {
        show(CameraAccess.Granted, unavailable = true)

        composeRule.onNodeWithText("No camera found").assertIsDisplayed()
        composeRule.onNodeWithText("Type it instead").performClick()
        assertEquals(listOf("type"), calls)
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theBackButtonClosesEveryNotice() {
        show(CameraAccess.NotAsked)

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("close"), calls)
    }
}
