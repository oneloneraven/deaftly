package com.ravendarque.deaftly.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.LocalDarkTheme
import com.ravendarque.deaftly.ui.theme.colours
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TrackWidth = 96.dp
private val TrackHeight = 44.dp
private val SwitchTarget = 48.dp
private val ThumbSize = 38.dp
private val ThumbInset = 3.dp
private val ThumbShadow = 4.dp
private val RimWidth = 1.dp
private val EdgeShadow = 10.dp
private const val SLIDE_MILLIS = 200
private const val SWITCH_THRESHOLD = 0.5f
private const val SHEEN_MIX = 0.4f
private const val SHEEN_X = 0.35f
private const val SHEEN_Y = 0.3f
private const val THUMB_INITIAL_SP = 16
private const val LIGHT_INNER_SHADOW = 0.22f
private const val DARK_INNER_SHADOW = 0.5f
private const val LIGHT_HIGHLIGHT = 0.7f
private const val DARK_HIGHLIGHT = 0.07f
internal const val INACTIVE_NAME_ALPHA = 0.6f

private fun ParticipantId.opposite() = if (this == ParticipantId.A) ParticipantId.B else ParticipantId.A

@Composable
fun TurnControl(
    conversation: Conversation,
    sidesSwapped: Boolean,
    onSelect: (ParticipantId) -> Unit,
    modifier: Modifier = Modifier,
    tail: BubbleTailState? = null,
) {
    val active = conversation.turn
    val left = if (sidesSwapped) conversation.participantA else conversation.participantB
    val right = if (sidesSwapped) conversation.participantB else conversation.participantA
    val haptic = LocalHapticFeedback.current
    val select by rememberUpdatedState(onSelect)
    val choose: (ParticipantId) -> Unit = { id ->
        if (id != conversation.turn) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            select(id)
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        NameButton(left.labelled(), left.id == active, Alignment.CenterEnd, { choose(left.id) }, Modifier.weight(1f), tail)
        ThumbSwitch(left.labelled(), right.labelled(), active, choose)
        NameButton(right.labelled(), right.id == active, Alignment.CenterStart, { choose(right.id) }, Modifier.weight(1f), tail)
    }
}

@Composable
private fun NameButton(
    participant: Participant,
    isActive: Boolean,
    alignment: Alignment,
    onClick: () -> Unit,
    modifier: Modifier,
    tail: BubbleTailState? = null,
) {
    val ink = Deaftly.colors.ink
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(
        modifier =
            modifier
                .heightIn(min = SwitchTarget)
                .clip(Deaftly.shapes.controlSmall)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { selected = isActive }
                .padding(horizontal = Deaftly.spacing.space8),
        contentAlignment = alignment,
    ) {
        Text(
            text = participant.displayName.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) ink else ink.copy(alpha = INACTIVE_NAME_ALPHA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout = it },
            modifier =
                if (isActive && tail != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        tail.name = coordinates.boundsInRoot()
                        tail.tipX =
                            layout
                                ?.let { innerLetterCentre(it, innerAtEnd = alignment == Alignment.CenterEnd) }
                                ?.let { coordinates.localToRoot(Offset(it, 0f)).x }
                    }
                } else {
                    Modifier
                },
        )
    }
}

private fun innerLetterCentre(
    layout: TextLayoutResult,
    innerAtEnd: Boolean,
): Float? {
    val visibleEnd = layout.getLineEnd(0, visibleEnd = true)
    if (visibleEnd == 0) return null
    return layout.getBoundingBox(if (innerAtEnd) visibleEnd - 1 else 0).center.x
}

@Composable
private fun ThumbSwitch(
    left: Participant,
    right: Participant,
    active: ParticipantId,
    choose: (ParticipantId) -> Unit,
) {
    val activeOnRight = active == right.id
    val position = remember { Animatable(if (activeOnRight) 1f else 0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(activeOnRight) { position.animateTo(if (activeOnRight) 1f else 0f, tween(SLIDE_MILLIS)) }
    val label = stringResource(R.string.chat_turn_label)
    val turnText = stringResource(R.string.chat_turn_of, (if (activeOnRight) right else left).displayName.orEmpty())
    val switchLabel = stringResource(R.string.chat_turn_switch_to, (if (activeOnRight) left else right).displayName.orEmpty())
    val travelPx = with(LocalDensity.current) { (TrackWidth - ThumbSize - ThumbInset * 2).toPx() }
    val sideOf = { onRight: Boolean -> if (onRight) right.id else left.id }
    Box(
        modifier =
            Modifier
                .size(TrackWidth, SwitchTarget)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    stateDescription = turnText
                    role = Role.Switch
                    liveRegion = LiveRegionMode.Polite
                    onClick(label = switchLabel) {
                        choose(active.opposite())
                        true
                    }
                }.pointerInput(travelPx, activeOnRight) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, delta ->
                            change.consume()
                            scope.launch { position.snapTo((position.value + delta / travelPx).coerceIn(0f, 1f)) }
                        },
                        onDragEnd = {
                            val toRight = position.value >= SWITCH_THRESHOLD
                            scope.launch { position.animateTo(if (toRight) 1f else 0f, tween(SLIDE_MILLIS)) }
                            choose(sideOf(toRight))
                        },
                        onDragCancel = { scope.launch { position.animateTo(if (activeOnRight) 1f else 0f, tween(SLIDE_MILLIS)) } },
                    )
                }.pointerInput(active) { detectTapGestures { choose(active.opposite()) } },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(TrackWidth, TrackHeight).recessedTrack(Deaftly.colors.surfaceSunken, LocalDarkTheme.current)) {
            val fraction = position.value
            val leftColours = left.colour.colours()
            val rightColours = right.colour.colours()
            val showRight = fraction >= SWITCH_THRESHOLD
            Thumb(
                fill = lerp(leftColours.strong, rightColours.strong, fraction),
                rim = lerp(leftColours.label, rightColours.label, fraction),
                initial = (if (showRight) right else left).avatarInitial.orEmpty(),
                onThumb = if (showRight) rightColours.onStrong else leftColours.onStrong,
                modifier = Modifier.offset { IntOffset((ThumbInset.toPx() + fraction * travelPx).roundToInt(), ThumbInset.roundToPx()) },
            )
        }
    }
}

@Composable
private fun Thumb(
    fill: Color,
    rim: Color,
    initial: String,
    onThumb: Color,
    modifier: Modifier,
) {
    val sheen = lerp(fill, Color.White, SHEEN_MIX)
    val sizePx = with(LocalDensity.current) { ThumbSize.toPx() }
    Box(
        modifier =
            modifier
                .size(ThumbSize)
                .shadow(ThumbShadow, CircleShape)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(sheen, fill), center = Offset(sizePx * SHEEN_X, sizePx * SHEEN_Y), radius = sizePx))
                .border(RimWidth, rim, CircleShape)
                .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = onThumb,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = THUMB_INITIAL_SP.sp),
        )
    }
}

private fun Modifier.recessedTrack(
    fill: Color,
    dark: Boolean,
): Modifier =
    drawBehind {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(fill, cornerRadius = corner)
        val shadow = Color.Black.copy(alpha = if (dark) DARK_INNER_SHADOW else LIGHT_INNER_SHADOW)
        val highlight = Color.White.copy(alpha = if (dark) DARK_HIGHLIGHT else LIGHT_HIGHLIGHT)
        val edge = EdgeShadow.toPx()
        clipPath(Path().apply { addRoundRect(RoundRect(size.toRect(), corner)) }) {
            drawRect(Brush.verticalGradient(listOf(shadow, Color.Transparent), startY = 0f, endY = edge), size = Size(size.width, edge))
            drawRect(
                Brush.horizontalGradient(listOf(shadow, Color.Transparent), startX = 0f, endX = edge),
                size = Size(edge, size.height),
            )
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, shadow), startX = size.width - edge, endX = size.width),
                topLeft = Offset(size.width - edge, 0f),
                size = Size(edge, size.height),
            )
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, highlight), startY = size.height - 3.dp.toPx(), endY = size.height),
                topLeft = Offset(0f, size.height - 3.dp.toPx()),
                size = Size(size.width, 3.dp.toPx()),
            )
        }
    }
