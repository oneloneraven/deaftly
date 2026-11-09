package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w390dp-h844dp")
class HintedFieldDecoratorTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val hint = "Type to the person opposite and keep going until it wraps"
    private val caret = Color(0xFFFF00FF)

    private fun showEmptyFieldWithWrappingHint() {
        val focus = FocusRequester()
        composeRule.setContent {
            AppTheme {
                val style = MaterialTheme.typography.bodyLarge
                Box(Modifier.width(220.dp)) {
                    BasicTextField(
                        state = TextFieldState(),
                        modifier = Modifier.focusRequester(focus),
                        textStyle = style,
                        cursorBrush = SolidColor(caret),
                        decorator = hintedFieldDecorator(true, hint, style, Color.Gray, 48.dp),
                    )
                }
            }
        }
        composeRule.runOnIdle { focus.requestFocus() }
        composeRule.waitForIdle()
    }

    private fun assertCaretSitsOnTheHintsFirstLine() {
        showEmptyFieldWithWrappingHint()
        val hintNode = composeRule.onNodeWithText(hint, useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.runOnUiThread {
            hintNode.config
                .getOrNull(SemanticsActions.GetTextLayoutResult)
                ?.action
                ?.invoke(layouts)
        }
        val layout = layouts.single()
        assertTrue(layout.lineCount >= 2, "the hint should wrap")
        val firstLineHeight = layout.getLineBottom(0) - layout.getLineTop(0)

        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val rows = (0 until pixels.height).filter { y -> (0 until pixels.width).any { x -> pixels[x, y] == caret } }
        assertTrue(rows.isNotEmpty(), "the caret should be drawn")

        assertEquals(hintNode.boundsInRoot.top, rows.first().toFloat(), 2f, "caret top")
        assertEquals(firstLineHeight, rows.size.toFloat(), 3f, "caret height")
    }

    @Test
    fun theEmptyFieldsCaretSitsOnTheHintsFirstLineAtItsHeight() = assertCaretSitsOnTheHintsFirstLine()

    @Test
    @Config(fontScale = 2f)
    fun theEmptyFieldsCaretSitsOnTheHintsFirstLineAtLargeFontScale() = assertCaretSitsOnTheHintsFirstLine()
}
