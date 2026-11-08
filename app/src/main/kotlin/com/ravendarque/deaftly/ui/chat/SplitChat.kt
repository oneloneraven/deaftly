package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.colours

private const val HALF_TINT = 0.12f
private const val HALF_ROTATION = 180f
private const val BAR_LETTER_SPACING_EM = 0.04f
private val BarTarget = 48.dp
private val DotSize = 8.dp
private val FieldRadius = 20.dp
private val FieldBorder = 2.5.dp

data class SplitActions(
    val onChange: (Composition) -> Unit,
    val onSend: () -> Unit,
    val onSelectTurn: (ParticipantId) -> Unit,
    val onEnd: () -> Unit,
)

@Composable
fun SplitChat(
    state: ChatUiState.Ready,
    actions: SplitActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Half(ParticipantId.B, state, actions, rotated = true, Modifier.weight(1f))
            TurnBar(state, actions.onEnd)
            Half(ParticipantId.A, state, actions, rotated = false, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Half(
    owner: ParticipantId,
    state: ChatUiState.Ready,
    actions: SplitActions,
    rotated: Boolean,
    modifier: Modifier,
) {
    val conversation = state.conversation
    val person = conversation.participant(owner).labelled()
    val name = person.displayName.orEmpty()
    val colours = person.colour.colours()
    val ground = Deaftly.colors.ground
    val tint = if (rotated) lerp(ground, colours.container, HALF_TINT) else ground
    val label = stringResource(if (rotated) R.string.chat_split_half_other else R.string.chat_split_half_own, name)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { if (rotated) rotationZ = HALF_ROTATION }
                .background(tint)
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = if (rotated) 0f else 2f
                    contentDescription = label
                }.padding(horizontal = Deaftly.spacing.space14, vertical = Deaftly.spacing.space12),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10),
    ) {
        MessageList(conversation, state.textSize, onQuote = null, modifier = Modifier.weight(1f).fillMaxWidth(), reader = owner)
        Box(Modifier.semantics { traversalIndex = 1f }) {
            if (conversation.turn == owner) {
                ActiveField(owner, state, actions)
            } else {
                Waiting(
                    conversation.activeParticipant
                        .labelled()
                        .displayName
                        .orEmpty(),
                ) { actions.onSelectTurn(owner) }
            }
        }
    }
}

@Composable
private fun ActiveField(
    owner: ParticipantId,
    state: ChatUiState.Ready,
    actions: SplitActions,
) {
    val participant = state.conversation.participant(owner)
    val colours = participant.colour.colours()
    val composer = rememberComposerState(owner, state.draft.composition, actions.onChange)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canSend = !composer.composition.isBlank
    val send = {
        if (canSend) {
            actions.onSend()
        }
    }
    val shape = RoundedCornerShape(FieldRadius)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Deaftly.colors.surface, shape)
                .border(FieldBorder, colours.strong, shape)
                .padding(start = Deaftly.spacing.space14, top = Deaftly.spacing.space4, end = Deaftly.spacing.space4)
                .padding(bottom = Deaftly.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        MessageField(
            state = composer,
            owner = participant.labelled().displayName.orEmpty(),
            cursor = colours.strong,
            onSend = send,
            modifier = Modifier.weight(1f).focusRequester(focus),
            hint = stringResource(R.string.chat_split_hint),
        )
        SendButton(enabled = canSend, onClick = send, container = colours.strong, content = colours.onStrong)
    }
}

@Composable
private fun Waiting(
    activeName: String,
    onTakeTurn: () -> Unit,
) {
    val c = Deaftly.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surfaceSunken, RoundedCornerShape(FieldRadius))
                .padding(start = Deaftly.spacing.space14, top = Deaftly.spacing.space4, end = Deaftly.spacing.space4)
                .padding(bottom = Deaftly.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        Text(
            text = stringResource(R.string.chat_split_waiting, activeName),
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkMuted,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .heightIn(min = BarTarget)
                    .clip(Deaftly.shapes.controlSmall)
                    .background(c.surface)
                    .clickable(role = Role.Button, onClick = onTakeTurn)
                    .padding(horizontal = Deaftly.spacing.space14),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.chat_split_my_turn), style = MaterialTheme.typography.labelLarge, color = c.ink)
        }
    }
}

@Composable
private fun TurnBar(
    state: ChatUiState.Ready,
    onEnd: () -> Unit,
) {
    val c = Deaftly.colors
    val active = state.conversation.activeParticipant.labelled()
    val turnText = stringResource(R.string.chat_split_turn, active.displayName.orEmpty()).uppercase()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.ink)
                .padding(horizontal = Deaftly.spacing.space12)
                .semantics { traversalIndex = 1f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val endLabel = stringResource(R.string.chat_end)
        Box(
            modifier =
                Modifier
                    .size(BarTarget)
                    .clip(Deaftly.shapes.controlSmall)
                    .clickable(role = Role.Button, onClick = onEnd)
                    .semantics { contentDescription = endLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(DeaftlyIcons.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.ground)
        }
        Row(
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(DotSize).background(active.colour.colours().strong, CircleShape))
            Text(
                text = turnText,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = BAR_LETTER_SPACING_EM.em),
                color = c.ground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.size(BarTarget))
    }
}
