package com.ravendarque.deaftly.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.colours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TrackHeight = 60.dp
private val ThumbInset = 4.dp
private val ChevronSize = 18.dp
private const val SWITCH_THRESHOLD = 0.5f
private const val NAME_ONLY_FONT_SCALE = 1.5f

private fun ParticipantId.fraction() = if (this == ParticipantId.A) 1f else 0f

private fun ParticipantId.opposite() = if (this == ParticipantId.A) ParticipantId.B else ParticipantId.A

private fun sideAt(fraction: Float) = if (fraction >= SWITCH_THRESHOLD) ParticipantId.A else ParticipantId.B

private fun Modifier.turnGestures(
    position: Animatable<Float, AnimationVector1D>,
    active: ParticipantId,
    travelPx: Float,
    scope: CoroutineScope,
    select: (ParticipantId) -> Unit,
): Modifier =
    pointerInput(travelPx, active) {
        detectHorizontalDragGestures(
            onHorizontalDrag = { change, delta ->
                change.consume()
                scope.launch { position.snapTo((position.value + delta / travelPx).coerceIn(0f, 1f)) }
            },
            onDragEnd = {
                val target = sideAt(position.value)
                scope.launch { position.animateTo(target.fraction()) }
                select(target)
            },
            onDragCancel = { scope.launch { position.animateTo(active.fraction()) } },
        )
    }.pointerInput(active) {
        detectTapGestures { offset ->
            val tapped = if (offset.x >= size.width / 2f) ParticipantId.A else ParticipantId.B
            if (tapped != active) select(tapped)
        }
    }

@Composable
fun TurnControl(
    conversation: Conversation,
    onSelect: (ParticipantId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = conversation.turn
    val position = remember { Animatable(active.fraction()) }
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(active) { position.animateTo(active.fraction()) }
    val label = stringResource(R.string.chat_turn_label)
    val activeName =
        conversation
            .participant(active)
            .labelled()
            .displayName
            .orEmpty()
    val switchLabel =
        stringResource(
            R.string.chat_turn_switch_to,
            conversation
                .participant(active.opposite())
                .labelled()
                .displayName
                .orEmpty(),
        )
    val thumbSide = sideAt(position.value)
    val thumbColours = conversation.participant(thumbSide).colour.colours()

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .background(Deaftly.colors.surfaceSunken, RoundedCornerShape(TrackHeight / 2))
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    stateDescription = activeName
                    role = Role.Switch
                    onClick(label = switchLabel) {
                        select(active.opposite())
                        true
                    }
                },
    ) {
        val thumbWidth = (maxWidth - ThumbInset * 2) / 2
        val travelPx = with(LocalDensity.current) { thumbWidth.toPx() }
        Thumb(thumbWidth, thumbColours.strong) { (ThumbInset.toPx() + position.value * travelPx).roundToInt() }
        Row(
            modifier = Modifier.fillMaxSize().turnGestures(position, active, travelPx, scope) { select(it) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Segment(conversation.participantB.labelled(), ParticipantId.B, thumbSide, thumbColours.onStrong, Modifier.weight(1f))
            Segment(conversation.participantA.labelled(), ParticipantId.A, thumbSide, thumbColours.onStrong, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Thumb(
    width: Dp,
    colour: Color,
    offsetX: Density.() -> Int,
) {
    val height = TrackHeight - ThumbInset * 2
    Box(
        modifier =
            Modifier
                .offset { IntOffset(offsetX(), ThumbInset.roundToPx()) }
                .width(width)
                .height(height)
                .background(colour, RoundedCornerShape(height / 2)),
    )
}

@Composable
private fun Segment(
    participant: Participant,
    side: ParticipantId,
    thumbSide: ParticipantId,
    onThumb: Color,
    modifier: Modifier,
) {
    val isActive = side == thumbSide
    val name = participant.displayName.orEmpty()
    Row(
        modifier = modifier.padding(horizontal = Deaftly.spacing.space8),
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isActive && side == ParticipantId.B) {
            Icon(DeaftlyIcons.Back, contentDescription = null, modifier = Modifier.size(ChevronSize), tint = Deaftly.colors.inkSecondary)
        }
        Text(
            text =
                if (isActive ||
                    LocalDensity.current.fontScale >= NAME_ONLY_FONT_SCALE
                ) {
                    name
                } else {
                    stringResource(R.string.chat_turn_swipe_to, name)
                },
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) onThumb else Deaftly.colors.inkSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (!isActive && side == ParticipantId.A) {
            Icon(DeaftlyIcons.Chevron, contentDescription = null, modifier = Modifier.size(ChevronSize), tint = Deaftly.colors.inkSecondary)
        }
    }
}
