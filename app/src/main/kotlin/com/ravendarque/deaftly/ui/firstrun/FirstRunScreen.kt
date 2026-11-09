package com.ravendarque.deaftly.ui.firstrun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.ui.chat.ColourChoices
import com.ravendarque.deaftly.ui.chat.NameField
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.settings.ChoiceRow
import com.ravendarque.deaftly.ui.settings.Section
import com.ravendarque.deaftly.ui.theme.Deaftly

private val PrivacyIconSize = 24.dp

@Composable
fun FirstRunScreen(
    viewModel: FirstRunViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FirstRunContent(
        state = state,
        onName = viewModel::setName,
        onColour = viewModel::setColour,
        onLayout = viewModel::setLayout,
        onGetStarted = { viewModel.getStarted(onDone) },
    )
}

@Composable
fun FirstRunContent(
    state: FirstRunState,
    onName: (String) -> Unit,
    onColour: (ColourRole) -> Unit,
    onLayout: (ChatLayout) -> Unit,
    onGetStarted: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space24),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space24),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
                    Text(
                        text = stringResource(R.string.first_run_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Deaftly.colors.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.first_run_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Deaftly.colors.inkSecondary,
                    )
                }
                Section(stringResource(R.string.first_run_me_heading)) {
                    NameField(
                        value = state.name,
                        placeholder = stringResource(R.string.chat_name_me),
                        onValueChange = onName,
                    )
                    ColourChoices(selected = state.colour, takenBy = null, onChoose = onColour)
                }
                LayoutChoices(state.layout, onLayout)
                PrivacyNote()
            }
            PrimaryButton(
                text = stringResource(R.string.first_run_start),
                onClick = onGetStarted,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Deaftly.spacing.space20)
                        .padding(top = Deaftly.spacing.space8, bottom = Deaftly.spacing.space24),
            )
        }
    }
}

@Composable
private fun LayoutChoices(
    layout: ChatLayout,
    onLayout: (ChatLayout) -> Unit,
) {
    Section(stringResource(R.string.settings_layout_heading)) {
        ChoiceRow(
            selected = layout == ChatLayout.Sides,
            label = stringResource(R.string.settings_layout_sides),
            hint = stringResource(R.string.settings_layout_sides_hint),
            onClick = { onLayout(ChatLayout.Sides) },
        )
        ChoiceRow(
            selected = layout == ChatLayout.Split,
            label = stringResource(R.string.settings_layout_split),
            hint = stringResource(R.string.settings_layout_split_hint),
            onClick = { onLayout(ChatLayout.Split) },
        )
    }
}

@Composable
private fun PrivacyNote() {
    val shape = Deaftly.shapes.control
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Deaftly.colors.accentSoft, shape)
                .padding(Deaftly.spacing.space16),
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = DeaftlyIcons.Home,
            contentDescription = null,
            tint = Deaftly.colors.accentStrong,
            modifier = Modifier.size(PrivacyIconSize),
        )
        Text(
            text = stringResource(R.string.first_run_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = Deaftly.colors.ink,
        )
    }
}
