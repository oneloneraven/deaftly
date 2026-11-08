package com.ravendarque.deaftly.ui.editor

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.ui.theme.Deaftly

private val FieldMinHeight = 52.dp
private val TargetSize = 48.dp
private val FocusedBorder = 2.dp
private val RestingBorder = 1.5.dp
private val ToolIconSize = 18.dp
private val FieldTextSize = 18.sp

@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = Deaftly.colors.inkMuted,
        modifier = modifier,
    )
}

@Composable
fun FieldFrame(
    focused: Boolean,
    invalid: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = FieldMinHeight,
    shape: Shape = Deaftly.shapes.control,
    alignment: Alignment = Alignment.CenterStart,
    content: @Composable () -> Unit,
) {
    val c = Deaftly.colors
    val borderColor =
        when {
            invalid -> c.error
            focused -> c.accent
            else -> c.outline
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(c.surface, shape)
                .border(if (focused || invalid) FocusedBorder else RestingBorder, borderColor, shape)
                .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space12),
        contentAlignment = alignment,
    ) {
        content()
    }
}

@Composable
fun LabelledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    val c = Deaftly.colors
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics { if (error != null) error(error) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = FieldTextSize, color = c.ink),
        singleLine = true,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
                FieldLabel(label)
                FieldFrame(focused = focused, invalid = error != null) {
                    if (value.isEmpty() && hint != null) {
                        Text(text = hint, style = MaterialTheme.typography.bodyMedium.copy(fontSize = FieldTextSize), color = c.inkMuted)
                    }
                    inner()
                }
                error?.let { FieldError(it) }
            }
        },
    )
}

@Composable
fun FieldError(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = Deaftly.colors.error, modifier = modifier)
}

@Composable
fun ToolButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.pill
    Row(
        modifier =
            modifier
                .heightIn(min = TargetSize)
                .clip(shape)
                .background(c.surface, shape)
                .border(RestingBorder, c.outline, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Deaftly.spacing.space16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ToolIconSize), tint = c.ink)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = c.ink,
        )
    }
}

@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Deaftly.colors.accent,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = TargetSize)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Deaftly.spacing.space4),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
            color = color,
        )
    }
}

data class ConfirmSpec(
    val title: String,
    val text: String,
    val confirm: String,
    val dismiss: String,
    val destructive: Boolean = false,
)

@Composable
fun ConfirmDialog(
    spec: ConfirmSpec,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Deaftly.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.ink,
        textContentColor = c.inkSecondary,
        title = { Text(spec.title) },
        text = { Text(spec.text) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = TargetSize)) {
                Text(spec.confirm, color = if (spec.destructive) c.error else c.accentStrong, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = TargetSize)) {
                Text(spec.dismiss, color = c.ink)
            }
        },
    )
}

@Composable
fun editorFieldColors() =
    Deaftly.colors.let { c ->
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.ink,
            unfocusedTextColor = c.ink,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.outline,
            focusedLabelColor = c.accentStrong,
            unfocusedLabelColor = c.inkMuted,
            cursorColor = c.accent,
        )
    }
