package com.ravendarque.deaftly.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val TailWidth = 26.dp
private val TipGap = 3.dp
private val LeanRange = 60.dp
private const val TAIL_MILLIS = 200
private const val LEAN_SHARE = 0.35f
private const val CENTRE_SHARE = 0.5f
private const val TOP_START_ANGLE = -90f
private const val RIGHT_START_ANGLE = 0f
private const val BOTTOM_START_ANGLE = 90f
private const val LEFT_START_ANGLE = 180f
private const val CORNER_SWEEP = 90f

@Stable
class BubbleTailState {
    var cardOrigin by mutableStateOf(Offset.Zero)
    var name by mutableStateOf<Rect?>(null)
}

@Composable
internal fun Modifier.speechBubble(
    tail: BubbleTailState?,
    shape: CornerBasedShape,
    fill: Color,
    border: Color,
    borderWidth: Dp,
): Modifier {
    val target = tail?.name?.center?.x
    val tipX = remember { Animatable(0f) }
    var seeded by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (target != null) {
            if (seeded) {
                tipX.animateTo(target, tween(TAIL_MILLIS))
            } else {
                tipX.snapTo(target)
                seeded = true
            }
        }
    }
    val positioned = if (tail == null) this else onGloballyPositioned { tail.cardOrigin = it.positionInRoot() }
    return positioned.drawBehind {
        val points = if (tail != null && seeded) tailPoints(tail, tipX.value, size.width, shape) else null
        val path = bubblePath(size, shape, borderWidth.toPx(), points)
        drawPath(path, fill)
        drawPath(path, border, style = Stroke(width = borderWidth.toPx(), join = StrokeJoin.Round))
    }
}

private class TailPoints(
    val tip: Offset,
    val baseLeft: Float,
    val baseRight: Float,
)

private fun DrawScope.tailPoints(
    tail: BubbleTailState,
    tipRootX: Float,
    width: Float,
    shape: CornerBasedShape,
): TailPoints? {
    val name = tail.name ?: return null
    val x = tipRootX - tail.cardOrigin.x
    val tipY = name.top - tail.cardOrigin.y - TipGap.toPx()
    val lean = ((x - width / 2) / LeanRange.toPx()).coerceIn(-1f, 1f)
    val tailWidth = TailWidth.toPx()
    val radius = shape.topStart.toPx(Size(width, width), this)
    val baseLeft = (x - (CENTRE_SHARE + LEAN_SHARE * lean) * tailWidth).coerceIn(radius, width - radius - tailWidth)
    return TailPoints(Offset(x, tipY), baseLeft, baseLeft + tailWidth)
}

private fun DrawScope.bubblePath(
    size: Size,
    shape: CornerBasedShape,
    border: Float,
    tail: TailPoints?,
): Path {
    val inset = border / 2
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val radius = shape.topStart.toPx(size, this) - inset
    val diameter = radius * 2
    return Path().apply {
        moveTo(left + radius, top)
        lineTo(right - radius, top)
        arcTo(Rect(right - diameter, top, right, top + diameter), TOP_START_ANGLE, CORNER_SWEEP, false)
        lineTo(right, bottom - radius)
        arcTo(Rect(right - diameter, bottom - diameter, right, bottom), RIGHT_START_ANGLE, CORNER_SWEEP, false)
        if (tail != null && tail.tip.y > bottom) {
            lineTo(tail.baseRight, bottom)
            lineTo(tail.tip.x, tail.tip.y)
            lineTo(tail.baseLeft, bottom)
        }
        lineTo(left + radius, bottom)
        arcTo(Rect(left, bottom - diameter, left + diameter, bottom), BOTTOM_START_ANGLE, CORNER_SWEEP, false)
        lineTo(left, top + radius)
        arcTo(Rect(left, top, left + diameter, top + diameter), LEFT_START_ANGLE, CORNER_SWEEP, false)
        close()
    }
}
