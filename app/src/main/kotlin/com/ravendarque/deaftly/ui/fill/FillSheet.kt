package com.ravendarque.deaftly.ui.fill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.ui.components.DeaftlyBottomSheet
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val OptionHeight = 56.dp
private val OptionBorderSelected = 2.dp
private val OptionBorder = 1.5.dp
private val CheckSize = 22.dp
private val TileHeight = 96.dp
private val TileIconSize = 26.dp
private val OptionTextSize = 18.sp

@Composable
fun FillSheet(
    active: ActivePlaceholder,
    onChoose: (String) -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    DeaftlyBottomSheet(title = active.placeholder.label, onDismiss = onDismiss) {
        FillSheetContent(active, onChoose, onScan)
    }
}

@Composable
fun ColumnScope.FillSheetContent(
    active: ActivePlaceholder,
    onChoose: (String) -> Unit,
    onScan: () -> Unit,
) {
    key(active.placeholder.key) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space14),
        ) {
            when (active.placeholder.type) {
                PlaceholderType.Choices -> {
                    OptionList(active.placeholder.options, active.values, onChoose)
                    CustomValueField(label = stringResource(R.string.fill_something_else), autofocus = false, onChoose = onChoose)
                }

                PlaceholderType.Typing -> {
                    CustomValueField(label = stringResource(R.string.fill_type_here), autofocus = true, onChoose = onChoose)
                }

                PlaceholderType.Camera -> {
                    CameraChoice(onScan, onChoose)
                }
            }
        }
    }
}

@Composable
private fun OptionList(
    options: List<String>,
    selected: List<String>,
    onChoose: (String) -> Unit,
) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
        options.forEach { option ->
            OptionRow(option, selected = option in selected, onClick = { onChoose(option) })
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.control
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = OptionHeight)
                .clip(shape)
                .background(if (selected) c.accentSoft else c.surface, shape)
                .border(
                    if (selected) OptionBorderSelected else OptionBorder,
                    if (selected) c.accent else c.line,
                    shape,
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = OptionTextSize,
                    fontWeight = if (selected) FontWeight.Bold else null,
                ),
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(DeaftlyIcons.Check, contentDescription = null, modifier = Modifier.size(CheckSize), tint = c.accent)
        }
    }
}

@Composable
private fun CameraChoice(
    onScan: () -> Unit,
    onChoose: (String) -> Unit,
) {
    var typing by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12)) {
        ActionTile(
            stringResource(R.string.fill_scan_menu),
            DeaftlyIcons.Scan,
            dark = true,
            onClick = onScan,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            stringResource(R.string.fill_type_it),
            DeaftlyIcons.Type,
            dark = false,
            onClick = { typing = true },
            modifier = Modifier.weight(1f),
        )
    }
    if (typing) {
        CustomValueField(label = stringResource(R.string.fill_type_here), autofocus = true, onChoose = onChoose)
    }
}

@Composable
private fun ActionTile(
    text: String,
    icon: ImageVector,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.button
    val container = if (dark) c.ink else c.surface
    val content = if (dark) c.ground else c.ink
    Column(
        modifier =
            modifier
                .heightIn(min = TileHeight)
                .clip(shape)
                .background(container, shape)
                .then(if (dark) Modifier else Modifier.border(BorderStroke(OptionBorder, c.lineStrong), shape))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(Deaftly.spacing.space16),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(TileIconSize), tint = content)
        Text(text = text, style = MaterialTheme.typography.titleSmall, color = content)
    }
}

@Composable
private fun CustomValueField(
    label: String,
    autofocus: Boolean,
    onChoose: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val c = Deaftly.colors
    val submit = { if (value.isNotBlank()) onChoose(value) }
    if (autofocus) LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        label = { Text(label) },
        leadingIcon = { Icon(DeaftlyIcons.Edit, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                DeaftlyIconButton(DeaftlyIcons.Check, stringResource(R.string.fill_use_this), onClick = submit)
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = OptionTextSize),
        shape = Deaftly.shapes.control,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
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
                focusedLeadingIconColor = c.accentStrong,
                unfocusedLeadingIconColor = c.inkMuted,
                cursorColor = c.accent,
            ),
    )
}
