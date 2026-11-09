package com.ravendarque.deaftly.ui.ocr

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.ocr.OcrSelection
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.PhraseSelection
import com.ravendarque.deaftly.core.ocr.TextSelection
import com.ravendarque.deaftly.core.ocr.WordRef
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

    private fun camera(
        dark: Boolean,
        picked: List<Phrase> = emptyList(),
        range: TextSelection? = null,
        frozen: Boolean = false,
        torch: Boolean? = null,
    ) = shot(dark) {
        val selection =
            range?.let { OcrSelection().dragged(it) } ?: picked.fold(OcrSelection()) { chosen, phrase -> chosen.toggled(phrase) }
        OcrCameraContent(
            content =
                OcrContent(
                    status = if (frozen) "Frozen" else "Live",
                    phrases = menuPhrases,
                    selection = selection,
                    fieldText = range?.let { PhraseSelection.text(menuPhrases, it) } ?: selection.tappedText,
                    frozen = frozen,
                    frozenImage = if (frozen) fakeFrameImage() else null,
                    torch = torch,
                ),
            actions = OcrActions({}, {}, {}, {}, {}, {}, {}, {}),
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

    @Test
    fun selectedPhraseLightTheme() = camera(dark = false, picked = listOf(menuPhrases[1]))

    @Test
    fun selectedPhraseDarkTheme() = camera(dark = true, picked = listOf(menuPhrases[1]))

    @Test
    @Config(fontScale = 2f)
    fun selectedPhraseLargeFontScale() = camera(dark = false, picked = listOf(menuPhrases[1]))

    private val twoLines = listOf(menuPhrases[5], menuPhrases[1])

    @Test
    fun twoTappedLinesLightTheme() = camera(dark = false, picked = twoLines)

    @Test
    fun twoTappedLinesDarkTheme() = camera(dark = true, picked = twoLines)

    @Test
    @Config(fontScale = 2f)
    fun twoTappedLinesLargeFontScale() = camera(dark = false, picked = twoLines)

    private val acrossLines = TextSelection(WordRef(2, 1), WordRef(4, 1))

    @Test
    fun draggedRangeAcrossLinesLightTheme() = camera(dark = false, range = acrossLines)

    @Test
    fun draggedRangeAcrossLinesDarkTheme() = camera(dark = true, range = acrossLines)

    @Test
    @Config(fontScale = 2f)
    fun draggedRangeAcrossLinesLargeFontScale() = camera(dark = false, range = acrossLines)

    @Test
    fun frozenWithTorchOnLightTheme() = camera(dark = false, picked = listOf(menuPhrases[3]), frozen = true, torch = true)

    @Test
    fun frozenWithTorchOnDarkTheme() = camera(dark = true, picked = listOf(menuPhrases[3]), frozen = true, torch = true)

    @Test
    @Config(fontScale = 2f)
    fun frozenWithTorchOffLargeFontScale() = camera(dark = false, frozen = true, torch = false)

    @Test
    fun liveWithTorchOffLightTheme() = camera(dark = false, torch = false)
}
