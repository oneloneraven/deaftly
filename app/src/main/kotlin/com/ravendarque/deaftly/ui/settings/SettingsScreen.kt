package com.ravendarque.deaftly.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.core.settings.ThemeMode
import com.ravendarque.deaftly.ui.chat.ColourChoices
import com.ravendarque.deaftly.ui.chat.LabelledTextField
import com.ravendarque.deaftly.ui.chat.NameField
import com.ravendarque.deaftly.ui.chat.PARTICIPANT_NAME_MAX_LENGTH
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.editor.ConfirmDialog
import com.ravendarque.deaftly.ui.editor.ConfirmSpec
import com.ravendarque.deaftly.ui.show.labelRes
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.LocalDarkTheme
import com.ravendarque.deaftly.ui.theme.colours
import com.ravendarque.deaftly.ui.theme.defaultShowScheme

private val RowMinHeight = 56.dp
private val RowBorder = 1.5.dp
private val SwatchWidth = 48.dp
private val SwatchHeight = 36.dp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(title = stringResource(R.string.route_settings), onNavigate = onBack)
            when (val current = state) {
                SettingsUiState.Loading -> Unit
                is SettingsUiState.Ready -> SettingsContent(current, viewModel)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Ready,
    viewModel: SettingsViewModel,
) {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space24),
    ) {
        MeSection(state, viewModel)
        Section(stringResource(R.string.settings_appearance_heading)) {
            ThemeMode.entries.forEach { mode ->
                ChoiceRow(
                    selected = mode == state.themeMode,
                    label = stringResource(mode.labelRes()),
                    hint = if (mode == ThemeMode.System) stringResource(R.string.settings_appearance_system_hint) else null,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
        }
        Section(stringResource(R.string.settings_scheme_heading)) {
            val current = state.showScheme ?: defaultShowScheme(LocalDarkTheme.current)
            ShowScheme.entries.forEach { scheme ->
                ChoiceRow(
                    selected = scheme == current,
                    label = stringResource(scheme.labelRes()).replaceFirstChar { it.uppercase() },
                    hint = null,
                    onClick = { viewModel.setShowScheme(scheme) },
                    leading = { SchemeSwatch(scheme) },
                )
            }
        }
        RestoreBuiltIns(state, viewModel)
    }
}

@Composable
private fun RestoreBuiltIns(
    state: SettingsUiState.Ready,
    viewModel: SettingsViewModel,
) {
    Section(stringResource(R.string.settings_templates_heading)) {
        Text(
            text = stringResource(R.string.settings_restore_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Deaftly.colors.inkSecondary,
        )
        var askingAboutEdits by rememberSaveable { mutableStateOf(false) }
        SecondaryButton(
            text = stringResource(R.string.settings_restore_builtins),
            onClick = { if (state.editedBuiltIns > 0) askingAboutEdits = true else viewModel.restoreBuiltInTemplates() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.hiddenBuiltIns > 0 || state.editedBuiltIns > 0,
        )
        if (askingAboutEdits) {
            ConfirmDialog(
                spec =
                    ConfirmSpec(
                        title = stringResource(R.string.settings_restore_edited_title),
                        text = stringResource(R.string.settings_restore_edited_text),
                        confirm = stringResource(R.string.settings_restore_all),
                        dismiss = stringResource(R.string.settings_restore_keep_changes),
                        destructive = true,
                    ),
                onConfirm = {
                    askingAboutEdits = false
                    viewModel.restoreAllBuiltInTemplates()
                },
                onDismiss = {
                    askingAboutEdits = false
                    viewModel.restoreBuiltInTemplates()
                },
                onCancel = { askingAboutEdits = false },
            )
        }
    }
}

@Composable
private fun MeSection(
    state: SettingsUiState.Ready,
    viewModel: SettingsViewModel,
) {
    var name by rememberSaveable { mutableStateOf(state.profile.name.orEmpty()) }
    Section(stringResource(R.string.settings_me_heading)) {
        Text(
            text = stringResource(R.string.settings_names_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Deaftly.colors.inkSecondary,
        )
        NameInput(state.names.firstName, stringResource(R.string.settings_first_name_label)) { value ->
            viewModel.changeNames { it.copy(firstName = value) }
        }
        NameInput(state.names.lastName, stringResource(R.string.settings_last_name_label)) { value ->
            viewModel.changeNames { it.copy(lastName = value) }
        }
        CheckRow(
            checked = state.names.legalNameDiffers,
            label = stringResource(R.string.settings_legal_name_differs),
            onChange = { value -> viewModel.changeNames { it.copy(legalNameDiffers = value) } },
        )
        if (state.names.legalNameDiffers) {
            NameInput(state.names.legalName, stringResource(R.string.settings_legal_name_label)) { value ->
                viewModel.changeNames { it.copy(legalName = value) }
            }
        }
        CheckRow(
            checked = state.names.useFirstNameInChats,
            label = stringResource(R.string.settings_use_first_name),
            onChange = { value -> viewModel.changeNames { it.copy(useFirstNameInChats = value) } },
        )
        if (state.names.chatName == null) {
            NameField(
                value = name,
                placeholder = stringResource(R.string.chat_name_me),
                onValueChange = {
                    name = it.take(PARTICIPANT_NAME_MAX_LENGTH)
                    viewModel.setName(name)
                },
            )
        }
        ColourChoices(selected = state.profile.colour, takenBy = null, onChoose = viewModel::setColour)
    }
}

@Composable
internal fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Deaftly.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
internal fun ChoiceRow(
    selected: Boolean,
    label: String,
    hint: String?,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.control
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .clip(shape)
                .background(if (selected) c.accentSoft else c.surface, shape)
                .border(RowBorder, if (selected) c.accent else c.line, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = c.accentStrong, unselectedColor = c.outline),
        )
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = c.ink)
            if (hint != null) {
                Text(text = hint, style = MaterialTheme.typography.bodySmall, color = c.inkSecondary)
            }
        }
    }
}

@Composable
private fun NameInput(
    initial: String,
    label: String,
    onChange: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    LabelledTextField(
        value = text,
        label = label,
        supporting = null,
        onValueChange = {
            text = it
            onChange(it)
        },
    )
}

@Composable
internal fun CheckRow(
    checked: Boolean,
    label: String,
    onChange: (Boolean) -> Unit,
) {
    val c = Deaftly.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = c.accentStrong, uncheckedColor = c.outline, checkmarkColor = c.onAccent),
        )
        Text(text = label, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SchemeSwatch(scheme: ShowScheme) {
    val colours = scheme.colours
    Box(
        modifier =
            Modifier
                .size(width = SwatchWidth, height = SwatchHeight)
                .background(colours.background, Deaftly.shapes.controlSmall)
                .border(RowBorder, Deaftly.colors.outline, Deaftly.shapes.controlSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.settings_swatch_glyph), style = MaterialTheme.typography.labelLarge, color = colours.text)
    }
}

private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.System -> R.string.settings_appearance_system
        ThemeMode.Light -> R.string.settings_appearance_light
        ThemeMode.Dark -> R.string.settings_appearance_dark
    }
