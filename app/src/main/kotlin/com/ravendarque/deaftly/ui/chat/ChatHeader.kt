package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.colours

private val HeaderTarget = 48.dp
private val DividerWidth = 1.dp
private const val STACK_FONT_SCALE = 1.3f

@Composable
fun ChatHeader(
    conversation: Conversation,
    onEnd: () -> Unit,
    onEditParticipants: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val left = conversation.participantB.labelled()
    val right = conversation.participantA.labelled()
    val line = Deaftly.colors.line
    val description = stringResource(R.string.chat_edit_participants, left.displayName.orEmpty(), right.displayName.orEmpty())
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    val y = size.height - DividerWidth.toPx() / 2
                    drawLine(line, Offset(0f, y), Offset(size.width, y), DividerWidth.toPx())
                }.padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        DeaftlyIconButton(DeaftlyIcons.Close, stringResource(R.string.chat_end), onEnd)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = HeaderTarget)
                    .clip(Deaftly.shapes.controlSmall)
                    .clickable(role = Role.Button, onClick = onEditParticipants)
                    .semantics(mergeDescendants = true) { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            if (LocalDensity.current.fontScale >= STACK_FONT_SCALE) {
                Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
                    HeaderPerson(left)
                    HeaderPerson(right)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8, Alignment.CenterHorizontally),
                ) {
                    HeaderPerson(left, Modifier.weight(1f, fill = false))
                    Text(
                        text = stringResource(R.string.chat_and),
                        color = Deaftly.colors.inkMuted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    HeaderPerson(right, Modifier.weight(1f, fill = false))
                }
            }
        }
        Box(Modifier.size(HeaderTarget))
    }
}

@Composable
private fun HeaderPerson(
    participant: Participant,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4),
    ) {
        ParticipantAvatar(participant.avatarInitial.orEmpty(), participant.colour.colours())
        Text(
            text = participant.displayName.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = Deaftly.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
