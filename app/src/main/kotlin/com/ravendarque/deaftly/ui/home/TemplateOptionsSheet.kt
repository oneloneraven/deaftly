package com.ravendarque.deaftly.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.DeaftlyBottomSheet
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val OptionMinHeight = 48.dp
private val CheckSize = 22.dp

@Composable
fun TemplateOptionsSheet(
    template: HomeTemplate,
    categories: List<String>,
    onSetPinned: (Boolean) -> Unit,
    onSetCategory: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DeaftlyBottomSheet(title = template.title, onDismiss = onDismiss) {
        TemplateOptionsContent(
            template = template,
            categories = categories,
            onSetPinned = {
                onSetPinned(it)
                onDismiss()
            },
            onSetCategory = {
                onSetCategory(it)
                onDismiss()
            },
        )
    }
}

@Composable
fun ColumnScope.TemplateOptionsContent(
    template: HomeTemplate,
    categories: List<String>,
    onSetPinned: (Boolean) -> Unit,
    onSetCategory: (String) -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        SecondaryButton(
            text = stringResource(if (template.pinned) R.string.template_unpin else R.string.template_pin),
            onClick = { onSetPinned(!template.pinned) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.template_category_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Column(modifier = Modifier.selectableGroup()) {
            categories.forEach { category ->
                CategoryOption(category, selected = category == template.category, onClick = { onSetCategory(category) })
            }
        }
        if (creating) {
            NewCategoryField(onSave = onSetCategory)
        } else {
            SecondaryButton(
                text = stringResource(R.string.template_new_category),
                onClick = { creating = true },
                modifier = Modifier.fillMaxWidth(),
                icon = DeaftlyIcons.Plus,
            )
        }
    }
}

@Composable
private fun CategoryOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = OptionMinHeight)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Deaftly.colors.ink, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(DeaftlyIcons.Check, contentDescription = null, modifier = Modifier.size(CheckSize), tint = Deaftly.colors.accentStrong)
        }
    }
}

@Composable
private fun NewCategoryField(onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val c = Deaftly.colors
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.template_category_name)) },
        singleLine = true,
        shape = Deaftly.shapes.control,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onSave(name) }),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.outline,
                focusedLabelColor = c.accentStrong,
                unfocusedLabelColor = c.inkMuted,
                cursorColor = c.accent,
            ),
    )
    PrimaryButton(
        text = stringResource(R.string.template_category_save),
        onClick = { onSave(name) },
        modifier = Modifier.fillMaxWidth(),
        enabled = name.isNotBlank(),
    )
}
