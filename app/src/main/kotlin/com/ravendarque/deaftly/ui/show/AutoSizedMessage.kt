package com.ravendarque.deaftly.ui.show

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.ui.theme.AtkinsonHyperlegibleNext

internal const val MIN_SHOW_FONT_SP = 32
internal const val MAX_SHOW_FONT_SP = 160
private const val LINE_HEIGHT_RATIO = 1.16f
private val WhitespacePattern = Regex("""\s+""")

private fun showStyle(
    sizeSp: Int,
    color: Color,
) = TextStyle(
    color = color,
    fontFamily = AtkinsonHyperlegibleNext,
    fontWeight = FontWeight.Bold,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * LINE_HEIGHT_RATIO).sp,
    letterSpacing = (-0.01).em,
)

internal fun fitFontSizeSp(
    text: String,
    available: Constraints,
    density: Density,
    measurer: TextMeasurer,
): Int {
    val words = text.split(WhitespacePattern).filter { it.isNotEmpty() }.distinct()
    val wrapped = Constraints(maxWidth = available.maxWidth)

    fun fits(sizeSp: Int): Boolean {
        val style = showStyle(sizeSp, Color.Unspecified)
        val height = measurer.measure(text, style, constraints = wrapped, density = density).size.height
        if (height > available.maxHeight) return false
        return words.all { word ->
            measurer.measure(word, style, softWrap = false, density = density).size.width <= available.maxWidth
        }
    }

    var low = MIN_SHOW_FONT_SP
    var high = MAX_SHOW_FONT_SP
    while (low < high) {
        val middle = (low + high + 1) / 2
        if (fits(middle)) low = middle else high = middle - 1
    }
    return low
}

@Composable
fun AutoSizedMessage(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val sizeSp = remember(text, constraints, density) { fitFontSizeSp(text, constraints, density, measurer) }
        val viewportHeight = maxHeight
        Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(text = text, style = showStyle(sizeSp, color))
            }
        }
    }
}
