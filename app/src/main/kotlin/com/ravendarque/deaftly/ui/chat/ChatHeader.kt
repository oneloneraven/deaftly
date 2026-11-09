package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val HeaderTarget = 48.dp
private val DividerWidth = 1.dp

data class ChatHeaderActions(
    val onEnd: () -> Unit,
    val onEditParticipants: () -> Unit,
    val onCycleTextSize: () -> Unit,
    val onSwitchSides: () -> Unit,
)

@Composable
fun ChatHeader(
    textSize: ChatTextSize,
    actions: ChatHeaderActions,
    modifier: Modifier = Modifier,
) {
    val line = Deaftly.colors.line
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    val y = size.height - DividerWidth.toPx() / 2
                    drawLine(line, Offset(0f, y), Offset(size.width, y), DividerWidth.toPx())
                }.padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4),
    ) {
        DeaftlyIconButton(DeaftlyIcons.Close, stringResource(R.string.chat_end), actions.onEnd)
        Spacer(Modifier.weight(1f))
        TextSizeButton(textSize, actions.onCycleTextSize)
        DeaftlyIconButton(DeaftlyIcons.SwitchSides, stringResource(R.string.chat_switch_sides), actions.onSwitchSides)
        DeaftlyIconButton(DeaftlyIcons.Settings, stringResource(R.string.chat_setup), actions.onEditParticipants)
    }
}

@Composable
private fun TextSizeButton(
    textSize: ChatTextSize,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.chat_text_size)
    val state = stringResource(textSize.label)
    Box(
        modifier =
            Modifier
                .defaultMinSize(HeaderTarget, HeaderTarget)
                .clip(Deaftly.shapes.controlSmall)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = label
                    stateDescription = state
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.chat_text_size_glyph),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = textSize.glyphSp.sp),
            color = Deaftly.colors.ink,
        )
    }
}
