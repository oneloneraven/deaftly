package com.ravendarque.deaftly.ui.ocr

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.ocr"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class OcrScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private fun shot(
        dark: Boolean,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { content() } }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    private fun gate(
        access: CameraAccess,
        dark: Boolean,
    ) = shot(dark) {
        CameraGate(access, unavailable = false, actions = CameraGateActions({}, {}, {}, {})) {}
    }

    private fun camera(dark: Boolean) =
        shot(dark) {
            OcrCameraContent(
                status = "Live",
                onClose = {},
                controlsEnabled = false,
                viewfinder = { FakeCamera(it) },
            )
        }

    @Test
    fun explanationLightTheme() = gate(CameraAccess.NotAsked, dark = false)

    @Test
    fun explanationDarkTheme() = gate(CameraAccess.NotAsked, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun explanationLargeFontScale() = gate(CameraAccess.NotAsked, dark = false)

    @Test
    fun deniedLightTheme() = gate(CameraAccess.Denied, dark = false)

    @Test
    fun blockedLightTheme() = gate(CameraAccess.DeniedForGood, dark = false)

    @Test
    fun blockedDarkTheme() = gate(CameraAccess.DeniedForGood, dark = true)

    @Test
    @Config(fontScale = 2f)
    fun blockedLargeFontScale() = gate(CameraAccess.DeniedForGood, dark = false)

    @Test
    fun cameraLightTheme() = camera(dark = false)

    @Test
    fun cameraDarkTheme() = camera(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun cameraLargeFontScale() = camera(dark = false)
}
