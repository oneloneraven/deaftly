package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.ui.components.DeaftlyBottomSheet
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.colours

const val PARTICIPANT_NAME_MAX_LENGTH = 24

private val SwatchSize = 52.dp
private val SwatchRing = 3.dp
private val SwatchOutline = 1.5.dp
private val CheckSize = 24.dp
private const val UNAVAILABLE_ALPHA = 0.4f

@Composable
fun ParticipantsSheet(
    conversation: Conversation,
    onChange: (Participant) -> Unit,
    onDone: () -> Unit,
) {
    DeaftlyBottomSheet(title = stringResource(R.string.participants_title), onDismiss = onDone) {
        ParticipantsSheetContent(conversation, onChange, onDone)
    }
}

@Composable
fun ColumnScope.ParticipantsSheetContent(
    conversation: Conversation,
    onChange: (Participant) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space20),
    ) {
        ParticipantSection(
            participant = conversation.participantA,
            other = conversation.participantB,
            heading = stringResource(R.string.participants_you),
            onChange = onChange,
        )
        ParticipantSection(
            participant = conversation.participantB,
            other = conversation.participantA,
            heading = stringResource(R.string.participants_other),
            onChange = onChange,
        )
    }
    PrimaryButton(text = stringResource(R.string.participants_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ParticipantSection(
    participant: Participant,
    other: Participant,
    heading: String,
    onChange: (Participant) -> Unit,
) {
    var text by rememberSaveable(participant.id) { mutableStateOf(participant.name.orEmpty()) }
    val fallback =
        participant
            .copy(name = null)
            .labelled()
            .displayName
            .orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall,
            color = Deaftly.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        NameField(
            value = text,
            placeholder = fallback,
            onValueChange = {
                text = it.take(PARTICIPANT_NAME_MAX_LENGTH)
                onChange(participant.copy(name = text.ifBlank { null }))
            },
        )
        ColourChoices(
            selected = participant.colour,
            takenBy = other.labelled(),
            onChoose = { onChange(participant.copy(colour = it)) },
        )
    }
}

@Composable
internal fun NameField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    val c = Deaftly.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.participants_name_label)) },
        supportingText = { Text(stringResource(R.string.participants_name_default, placeholder)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = Deaftly.shapes.control,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
                focusedContainerColor = c.ground,
                unfocusedContainerColor = c.ground,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.outline,
                focusedLabelColor = c.accentStrong,
                unfocusedLabelColor = c.inkMuted,
                focusedSupportingTextColor = c.inkSecondary,
                unfocusedSupportingTextColor = c.inkSecondary,
                cursorColor = c.accent,
            ),
    )
}

@Composable
internal fun ColourChoices(
    selected: ColourRole,
    takenBy: Participant?,
    onChoose: (ColourRole) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ColourRole.entries.forEach { role ->
                val taken = role == takenBy?.colour
                Swatch(
                    role = role,
                    selected = role == selected,
                    takenBy = takenBy?.displayName.takeIf { taken },
                    onClick = { onChoose(role) },
                )
            }
        }
        Text(
            text = stringResource(R.string.participants_colour_selected, selected.label()),
            style = MaterialTheme.typography.bodySmall,
            color = Deaftly.colors.inkSecondary,
        )
    }
}

@Composable
private fun Swatch(
    role: ColourRole,
    selected: Boolean,
    takenBy: String?,
    onClick: () -> Unit,
) {
    val colours = role.colours()
    val c = Deaftly.colors
    val name = role.label()
    val usedBy = takenBy?.let { stringResource(R.string.participants_colour_used_by, it) }
    Box(
        modifier =
            Modifier
                .size(SwatchSize)
                .clip(CircleShape)
                .alpha(if (takenBy != null) UNAVAILABLE_ALPHA else 1f)
                .background(colours.container, CircleShape)
                .border(if (selected) SwatchRing else SwatchOutline, if (selected) c.ink else c.outline, CircleShape)
                .selectable(selected = selected, enabled = takenBy == null, role = Role.RadioButton, onClick = onClick)
                .semantics {
                    contentDescription = name
                    if (usedBy != null) stateDescription = usedBy
                },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(DeaftlyIcons.Check, contentDescription = null, modifier = Modifier.size(CheckSize), tint = colours.onContainer)
        }
    }
}
