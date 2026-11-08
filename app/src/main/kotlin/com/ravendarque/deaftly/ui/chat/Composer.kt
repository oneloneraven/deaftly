package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.colours

private val CardBorder = 2.5.dp
private val SendWidth = 52.dp
private val SendHeight = 44.dp
private val InputMinHeight = 44.dp
private const val DISABLED_ALPHA = 0.4f
private const val MAX_LINES = 4

@Composable
fun Composer(
    participant: Participant,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val named = participant.labelled()
    val colours = participant.colour.colours()
    val shape = Deaftly.shapes.card
    val c = Deaftly.colors
    var field by remember(participant.id) { mutableStateOf(TextFieldValue(draft, TextRange(draft.length))) }
    LaunchedEffect(draft) { if (draft != field.text) field = TextFieldValue(draft, TextRange(draft.length)) }
    val canSend = field.text.isNotBlank()
    val send = {
        if (canSend) {
            onSend()
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(c.surface, shape)
                .border(CardBorder, colours.strong, shape)
                .padding(
                    start = Deaftly.spacing.space12,
                    end = Deaftly.spacing.space12,
                    top = Deaftly.spacing.space12,
                    bottom = Deaftly.spacing.space10,
                ),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        Row(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
        ) {
            Icon(DeaftlyIcons.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = c.ink)
            Text(
                text =
                    stringResource(
                        if (participant.id == ParticipantId.B) R.string.chat_turn_prompt_other else R.string.chat_turn_prompt_own,
                    ),
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
            )
        }
        MessageField(field, named.displayName.orEmpty(), colours.strong, {
            field = it
            onDraftChange(it.text)
        }, send)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SendButton(enabled = canSend, onClick = send, container = colours.strong, content = colours.onStrong)
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
) {
    val label = stringResource(R.string.chat_send)
    Box(
        modifier =
            Modifier
                .size(width = SendWidth, height = SendHeight)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(RoundedCornerShape(14.dp))
                .background(container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(DeaftlyIcons.Send, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MessageField(
    value: TextFieldValue,
    owner: String,
    cursor: Color,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
) {
    val c = Deaftly.colors
    val label = stringResource(R.string.chat_composer_label, owner)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = InputMinHeight).semantics { contentDescription = label },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
        cursorBrush = SolidColor(cursor),
        maxLines = MAX_LINES,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        decorationBox = { inner ->
            Box(Modifier.heightIn(min = InputMinHeight), contentAlignment = Alignment.CenterStart) {
                if (value.text.isEmpty()) {
                    Text(stringResource(R.string.chat_composer_hint), style = MaterialTheme.typography.bodyLarge, color = c.inkMuted)
                }
                inner()
            }
        },
    )
}
