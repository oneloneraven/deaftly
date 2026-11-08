package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.ui.theme.Deaftly
import kotlin.math.hypot

private val OutlineWidth = 2.dp
private val OutlineGrow = 3.dp
private const val OUTLINE_ALPHA = 0.9f

@Composable
fun PhraseOverlay(
    phrases: List<Phrase>,
    modifier: Modifier = Modifier,
) {
    val colour = Deaftly.colors.highlight.copy(alpha = OUTLINE_ALPHA)
    Canvas(modifier = modifier) {
        val grow = OutlineGrow.toPx()
        val stroke = Stroke(width = OutlineWidth.toPx(), join = StrokeJoin.Round)
        phrases.forEach { phrase ->
            drawPath(outline(phrase.quad, size.width, size.height, grow), colour, style = stroke)
        }
    }
}

internal fun outline(
    quad: Quad,
    width: Float,
    height: Float,
    grow: Float,
): Path {
    val corners = quad.points.map { Offset(it.x * width, it.y * height) }
    val centre = Offset(corners.map { it.x }.average().toFloat(), corners.map { it.y }.average().toFloat())
    val grown =
        corners.map { corner ->
            val away = corner - centre
            val length = hypot(away.x, away.y)
            if (length == 0f) corner else corner + away * (grow / length)
        }
    return Path().apply {
        moveTo(grown[0].x, grown[0].y)
        grown.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
}
