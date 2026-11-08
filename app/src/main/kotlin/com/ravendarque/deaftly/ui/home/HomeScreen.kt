package com.ravendarque.deaftly.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.ActionDock
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.InkButton
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onNavigate) {
            HomeActions(
                onNavigate,
                viewModel::onQueryChange,
                viewModel::onClearSearch,
                viewModel::onCategorySelected,
                viewModel::setPinned,
                viewModel::setCategory,
            )
        }
    HomeContent(state, actions)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
) {
    var optionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    val optionsTemplate = (state.pinned + state.templates).firstOrNull { it.id == optionsFor }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Header(actions.onNavigate)
            SearchField(state.query, actions.onQueryChange)
            TemplateList(state, actions, onOpenOptions = { optionsFor = it }, modifier = Modifier.weight(1f))
            ActionDock(
                start = { modifier ->
                    InkButton(
                        text = stringResource(R.string.home_type_and_show),
                        onClick = { actions.onNavigate(Route.Show) },
                        modifier = modifier,
                        icon = DeaftlyIcons.Type,
                    )
                },
                end = { modifier ->
                    SecondaryButton(
                        text = stringResource(R.string.home_start_chat),
                        onClick = { actions.onNavigate(Route.Chat) },
                        modifier = modifier,
                        icon = DeaftlyIcons.Chat,
                    )
                },
            )
        }
    }
    if (optionsTemplate != null) {
        TemplateOptionsSheet(
            template = optionsTemplate,
            categories = state.categories,
            onSetPinned = { actions.onSetPinned(optionsTemplate.id, it) },
            onSetCategory = { actions.onSetCategory(optionsTemplate.id, it) },
            onDismiss = { optionsFor = null },
        )
    }
}

@Composable
private fun Header(onNavigate: (Route) -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = Deaftly.spacing.space24,
                    top = Deaftly.spacing.space12,
                    end = Deaftly.spacing.space20,
                    bottom = Deaftly.spacing.space12,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
            DeaftlyIconButton(DeaftlyIcons.Plus, stringResource(R.string.home_new_template), onClick = { onNavigate(Route.Editor) })
            DeaftlyIconButton(DeaftlyIcons.Settings, stringResource(R.string.route_settings), onClick = { onNavigate(Route.Settings) })
        }
    }
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics { heading() },
    ) {
        Text(
            text = stringResource(R.string.wordmark),
            style = MaterialTheme.typography.headlineLarge,
        )
        Box(
            Modifier
                .padding(bottom = Deaftly.spacing.space8)
                .size(9.dp)
                .background(Deaftly.colors.accent, CircleShape),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val c = Deaftly.colors
    val focusManager: FocusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space4),
        label = { Text(stringResource(R.string.home_search_label)) },
        leadingIcon = { Icon(DeaftlyIcons.Search, contentDescription = null) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = Deaftly.shapes.control,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
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
