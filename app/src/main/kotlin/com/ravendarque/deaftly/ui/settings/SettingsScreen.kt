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
import androidx.compose.foundation.verticalScroll
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
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.ui.chat.ColourChoices
import com.ravendarque.deaftly.ui.chat.NameField
import com.ravendarque.deaftly.ui.chat.PARTICIPANT_NAME_MAX_LENGTH
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
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
    var name by rememberSaveable { mutableStateOf(state.profile.name.orEmpty()) }
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space24),
    ) {
        Section(stringResource(R.string.settings_layout_heading)) {
            ChoiceRow(
                selected = state.layout == ChatLayout.Sides,
                label = stringResource(R.string.settings_layout_sides),
                hint = stringResource(R.string.settings_layout_sides_hint),
                onClick = { viewModel.setLayout(ChatLayout.Sides) },
            )
            ChoiceRow(
                selected = state.layout == ChatLayout.Split,
                label = stringResource(R.string.settings_layout_split),
                hint = stringResource(R.string.settings_layout_split_hint),
                onClick = { viewModel.setLayout(ChatLayout.Split) },
            )
        }
        Section(stringResource(R.string.settings_me_heading)) {
            NameField(
                value = name,
                placeholder = stringResource(R.string.chat_name_me),
                onValueChange = {
                    name = it.take(PARTICIPANT_NAME_MAX_LENGTH)
                    viewModel.setName(name)
                },
            )
            ColourChoices(selected = state.profile.colour, takenBy = null, onChoose = viewModel::setColour)
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
