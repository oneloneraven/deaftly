package com.ravendarque.deaftly.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val SegmentHeight = 48.dp
private const val STACK_FONT_SCALE = 1.5f

@Composable
fun PlaceholderCard(
    draft: EditorDraft,
    placeholder: DraftPlaceholder,
    onEdit: ((EditorDraft) -> EditorDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deaftly.colors
    Column(
        modifier = modifier.fillMaxWidth().background(c.surface, Deaftly.shapes.card).padding(Deaftly.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space16),
    ) {
        CardHeader(placeholder, onDelete = { onEdit { it.deleteSelected() } })
        LabelledTextField(
            label = stringResource(R.string.editor_placeholder_name),
            value = draft.keyInput,
            onValueChange = { value -> onEdit { it.renameSelected(value) } },
            error = draft.keyError?.let { stringResource(it.messageRes()) },
        )
        LabelledTextField(
            label = stringResource(R.string.editor_placeholder_question),
            value = placeholder.label,
            onValueChange = { value -> onEdit { it.updateSelected { p -> p.copy(label = value) } } },
            hint = stringResource(R.string.editor_placeholder_question_hint),
            imeAction = ImeAction.Done,
        )
        FilledBy(placeholder.type) { type -> onEdit { it.updateSelected { p -> p.copy(type = type) } } }
        if (placeholder.type == PlaceholderType.Choices) {
            OptionsEditor(placeholder.options) { options -> onEdit { it.updateSelected { p -> p.copy(options = options) } } }
        }
        RepeatableSwitch(placeholder.repeatable) { value -> onEdit { it.updateSelected { p -> p.copy(repeatable = value) } } }
    }
}

private fun KeyError.messageRes() =
    when (this) {
        KeyError.Blank -> R.string.editor_error_key_blank
        KeyError.Braces -> R.string.editor_error_key_braces
        KeyError.Taken -> R.string.editor_error_key_taken
    }

@Composable
private fun CardHeader(
    placeholder: DraftPlaceholder,
    onDelete: () -> Unit,
) {
    val c = Deaftly.colors
    val camera = placeholder.type == PlaceholderType.Camera
    val title = stringResource(R.string.editor_placeholder_heading, placeholder.key)
    val keyStart = title.indexOf(placeholder.key)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text =
                buildAnnotatedString {
                    append(title.substring(0, keyStart))
                    withStyle(
                        SpanStyle(
                            background = if (camera) c.personB else c.accentSoft,
                            color = if (camera) c.onPersonB else c.accentStrong,
                        ),
                    ) { append(placeholder.key) }
                    append(title.substring(keyStart + placeholder.key.length))
                },
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        DeaftlyIconButton(DeaftlyIcons.Trash, stringResource(R.string.editor_delete_placeholder), onClick = onDelete)
    }
}

@Composable
private fun FilledBy(
    selected: PlaceholderType,
    onSelect: (PlaceholderType) -> Unit,
) {
    val c = Deaftly.colors
    val stacked = LocalDensity.current.fontScale >= STACK_FONT_SCALE
    val segments: @Composable (Modifier) -> Unit = { segmentModifier ->
        PlaceholderType.entries.forEach { type ->
            Segment(stringResource(type.labelRes()), type == selected, { onSelect(type) }, segmentModifier)
        }
    }
    val group =
        Modifier
            .fillMaxWidth()
            .clip(Deaftly.shapes.controlSmall)
            .background(c.surfaceSunken, Deaftly.shapes.controlSmall)
            .selectableGroup()
            .padding(Deaftly.spacing.space4)
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
        FieldLabel(stringResource(R.string.editor_filled_by))
        if (stacked) {
            Column(group, verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) { segments(Modifier.fillMaxWidth()) }
        } else {
            Row(group, horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) { segments(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.chip
    Box(
        modifier =
            modifier
                .heightIn(min = SegmentHeight)
                .clip(shape)
                .then(if (selected) Modifier.background(c.ink, shape) else Modifier)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = Deaftly.spacing.space8),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) c.ground else c.ink,
            textAlign = TextAlign.Center,
        )
    }
}

private fun PlaceholderType.labelRes() =
    when (this) {
        PlaceholderType.Choices -> R.string.editor_type_choices
        PlaceholderType.Typing -> R.string.editor_type_typing
        PlaceholderType.Camera -> R.string.editor_type_camera
    }

@Composable
private fun RepeatableSwitch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val c = Deaftly.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SegmentHeight)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
            Text(stringResource(R.string.editor_repeatable), style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(stringResource(R.string.editor_repeatable_hint), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = c.onAccent,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.inkMuted,
                    uncheckedTrackColor = c.surface,
                    uncheckedBorderColor = c.outline,
                ),
        )
    }
}
