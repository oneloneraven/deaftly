package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.core.ocr.DragGeometry
import com.ravendarque.deaftly.core.ocr.OcrSelection
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.PhraseHitTest
import com.ravendarque.deaftly.core.ocr.Point
import com.ravendarque.deaftly.core.ocr.Quad
import com.ravendarque.deaftly.core.ocr.SelectedRun
import com.ravendarque.deaftly.core.ocr.SelectionDrag
import com.ravendarque.deaftly.core.ocr.TextSelection
import com.ravendarque.deaftly.ui.theme.Deaftly
import kotlin.math.hypot
import kotlin.math.max

private val OutlineWidth = 2.dp
private val OutlineGrow = 3.dp
private val TouchSlop = 12.dp
private val MinTarget = 48.dp
private val HandleRadius = 7.dp
private val HandleGrab = 28.dp
private val HandleRing = 3.dp
private const val OUTLINE_ALPHA = 0.9f
private const val FILL_ALPHA = 0.4f
private const val TOP_LEFT = 0
private const val TOP_RIGHT = 1
private const val BOTTOM_LEFT = 3
private const val BOTTOM_RIGHT = 2

@Composable
fun PhraseOverlay(
    phrases: List<Phrase>,
    selection: OcrSelection,
    onTapPhrase: (Long?) -> Unit,
    onSelectRange: (TextSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val runs = selection.runs(phrases)
    val handleRange = selection.handleRange(phrases)
    BoxWithConstraints(modifier = modifier.selectionGestures(phrases, handleRange, onTapPhrase, onSelectRange)) {
        Highlights(phrases, runs, handleRange != null, Modifier.matchParentSize())
        PhraseNodes(
            phrases,
            runs.map { it.phrase.id }.toSet(),
            onTapPhrase,
            constraints.maxWidth.toFloat(),
            constraints.maxHeight.toFloat(),
        )
    }
}

@Composable
private fun Modifier.selectionGestures(
    phrases: List<Phrase>,
    selection: TextSelection?,
    onTapPhrase: (Long?) -> Unit,
    onSelectRange: (TextSelection) -> Unit,
): Modifier {
    val currentPhrases by rememberUpdatedState(phrases)
    val currentTap by rememberUpdatedState(onTapPhrase)
    val currentSelection by rememberUpdatedState(selection)
    val currentSelect by rememberUpdatedState(onSelectRange)
    return pointerInput(Unit) {
        val geometry = { DragGeometry(size.width.toFloat(), size.height.toFloat(), TouchSlop.toPx(), HandleGrab.toPx()) }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var active = SelectionDrag.begin(currentPhrases, currentSelection, down.position.toPoint(), geometry())
            var started = false
            drag(down.id) { change ->
                val range = active
                if (range != null) {
                    if (!started && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                        started = true
                        currentSelect(range)
                    }
                    if (started) {
                        val moved = SelectionDrag.move(currentPhrases, range, change.position.toPoint(), geometry())
                        active = moved
                        currentSelect(moved)
                        change.consume()
                    }
                }
            }
        }
    }.pointerInput(Unit) {
        detectTapGestures { at ->
            val hit =
                PhraseHitTest.phraseAt(currentPhrases, at.toPoint(), size.width.toFloat(), size.height.toFloat(), TouchSlop.toPx())
            currentTap(hit?.id)
        }
    }
}

@Composable
private fun PhraseNodes(
    phrases: List<Phrase>,
    selectedIds: Set<Long>,
    onTapPhrase: (Long?) -> Unit,
    width: Float,
    height: Float,
) {
    val density = LocalDensity.current
    val target = with(density) { MinTarget.toPx() }
    phrases.forEach { phrase ->
        val area = phrase.quad.boundingBox()
        val boxWidth = max(area.width * width, target)
        val boxHeight = max(area.height * height, target)
        val centreX = (area.left + area.right) / 2 * width
        val centreY = (area.top + area.bottom) / 2 * height
        Box(
            Modifier
                .offset { IntOffset((centreX - boxWidth / 2).toInt(), (centreY - boxHeight / 2).toInt()) }
                .size(with(density) { boxWidth.toDp() }, with(density) { boxHeight.toDp() })
                .semantics {
                    contentDescription = phrase.text
                    role = Role.Button
                    selected = phrase.id in selectedIds
                    onClick {
                        onTapPhrase(phrase.id)
                        true
                    }
                },
        )
    }
}

@Composable
private fun Highlights(
    phrases: List<Phrase>,
    runs: List<SelectedRun>,
    handles: Boolean,
    modifier: Modifier,
) {
    val outline = Deaftly.colors.highlight.copy(alpha = OUTLINE_ALPHA)
    val accent = Deaftly.colors.accent
    val handleFill = Color.White
    Canvas(modifier = modifier) {
        val grow = OutlineGrow.toPx()
        val stroke = Stroke(width = OutlineWidth.toPx(), join = StrokeJoin.Round)
        phrases.forEach { drawPath(outline(it.quad, size.width, size.height, grow), outline, style = stroke) }
        runs.forEach { run ->
            val quad = runQuad(run)
            val path = outline(quad, size.width, size.height, grow)
            drawPath(path, accent.copy(alpha = FILL_ALPHA))
            drawPath(path, accent, style = stroke)
        }
        if (handles) {
            runs.firstOrNull()?.let { handle(it.start, accent, handleFill) }
            runs.lastOrNull()?.let { handle(it.end, accent, handleFill) }
        }
    }
}

private fun DrawScope.handle(
    at: Point,
    ring: Color,
    fill: Color,
) {
    val centre = Offset(at.x * size.width, at.y * size.height)
    drawCircle(fill, HandleRadius.toPx(), centre)
    drawCircle(ring, HandleRadius.toPx() - HandleRing.toPx() / 2, centre, style = Stroke(HandleRing.toPx()))
}

private fun Offset.toPoint() = Point(x, y)

internal fun runQuad(run: SelectedRun): Quad {
    if (run.coversPhrase) return run.phrase.quad
    val first =
        run.phrase.words[run.firstWord]
            .quad.points
    val last =
        run.phrase.words[run.lastWord]
            .quad.points
    return Quad(listOf(first[TOP_LEFT], last[TOP_RIGHT], last[BOTTOM_RIGHT], first[BOTTOM_LEFT]))
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
