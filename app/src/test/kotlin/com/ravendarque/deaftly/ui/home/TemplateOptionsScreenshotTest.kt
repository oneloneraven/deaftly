package com.ravendarque.deaftly.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.core.template.RenderedPart
import com.ravendarque.deaftly.ui.components.SheetBody
import com.ravendarque.deaftly.ui.components.SheetHandle
import com.ravendarque.deaftly.ui.theme.AppTheme
import com.ravendarque.deaftly.ui.theme.Deaftly
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val SCREENSHOT_DIR = "src/test/screenshots"
private const val PACKAGE = "com.ravendarque.deaftly.ui.home"

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h844dp")
class TemplateOptionsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val testName = TestName()

    private val template =
        HomeTemplate("t", "Explain I'm deaf", "Everyday", pinned = false, preview = listOf(RenderedPart.Text("I'm deaf.")))
    private val categories = listOf("Everyday", "Food & drink", "Health", "Travel")

    @Composable
    private fun Sheet() {
        Surface(color = Deaftly.colors.surface, shape = Deaftly.shapes.sheet) {
            Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
                SheetBody(template.title) {
                    TemplateOptionsContent(template, categories, TemplateOptionActions({}, {}, {}, {}))
                }
            }
        }
    }

    private fun capture(
        dark: Boolean,
        beforeCapture: () -> Unit = {},
    ) {
        composeRule.setContent { AppTheme(darkTheme = dark) { Sheet() } }
        beforeCapture()
        composeRule.onRoot().captureRoboImage("$SCREENSHOT_DIR/$PACKAGE.${this::class.simpleName}.${testName.methodName}.png")
    }

    @Test
    fun lightTheme() = capture(dark = false)

    @Test
    fun darkTheme() = capture(dark = true)

    @Test
    @Config(fontScale = 2f)
    fun largeFontScale() = capture(dark = false)

    @Test
    fun newCategoryField() = capture(dark = false) { composeRule.onNodeWithText("New category…").performClick() }
}
