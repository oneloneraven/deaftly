package com.ravendarque.deaftly.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.BottomDock
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.editor.ConfirmDialog
import com.ravendarque.deaftly.ui.editor.ConfirmSpec
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun TemplatesScreen(
    viewModel: HomeViewModel,
    onNavigate: (Route) -> Unit,
    onHome: () -> Unit,
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
                viewModel::delete,
            )
        }
    TemplatesContent(state, actions, onHome)
}

@Composable
fun TemplatesContent(
    state: HomeUiState,
    actions: HomeActions,
    onHome: () -> Unit,
) {
    var optionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val all = state.pinned + state.templates
    val optionsTemplate = all.firstOrNull { it.id == optionsFor }
    val deletingTemplate = all.firstOrNull { it.id == deleting }
    val listActions = actions.copy(onDelete = { deleting = it })
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Header(onNewTemplate = { actions.onNavigate(Route.Editor()) })
            SearchField(state.query, actions.onQueryChange)
            TemplateList(state, listActions, onOpenOptions = { optionsFor = it }, modifier = Modifier.weight(1f))
            BottomDock {
                SecondaryButton(
                    text = stringResource(R.string.templates_home),
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                    icon = DeaftlyIcons.Home,
                )
            }
        }
    }
    if (optionsTemplate != null) {
        TemplateOptionsSheet(
            template = optionsTemplate,
            categories = state.categories,
            actions =
                TemplateOptionActions(
                    onSetPinned = { actions.onSetPinned(optionsTemplate.id, it) },
                    onSetCategory = { actions.onSetCategory(optionsTemplate.id, it) },
                    onEdit = { actions.onNavigate(Route.Editor(optionsTemplate.id)) },
                    onDelete = {
                        deleting = optionsTemplate.id
                        optionsFor = null
                    },
                ),
            onDismiss = { optionsFor = null },
        )
    }
    deletingTemplate?.let { template ->
        ConfirmDialog(
            spec =
                ConfirmSpec(
                    title = stringResource(R.string.editor_delete_title),
                    text = stringResource(R.string.editor_delete_text, template.title),
                    confirm = stringResource(R.string.editor_delete_confirm),
                    dismiss = stringResource(R.string.editor_dialog_cancel),
                    destructive = true,
                ),
            onConfirm = {
                actions.onDelete(template.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun Header(onNewTemplate: () -> Unit) {
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
        Text(
            text = stringResource(R.string.route_templates),
            style = MaterialTheme.typography.headlineLarge,
            color = Deaftly.colors.ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        DeaftlyIconButton(DeaftlyIcons.Plus, stringResource(R.string.home_new_template), onClick = onNewTemplate)
    }
}

@Composable
internal fun SearchField(
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
