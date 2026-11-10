package com.ravendarque.deaftly.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.CategoryChip
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val SaveHeight = 48.dp

private enum class EditorDialog { Discard, Delete, Restore }

data class EditorUiState(
    val draft: EditorDraft,
    val categories: List<String>,
    val isNew: Boolean,
    val builtIn: Boolean,
    val showErrors: Boolean,
    val dirty: Boolean,
)

data class EditorActions(
    val onEdit: ((EditorDraft) -> EditorDraft) -> Unit,
    val onBodyChange: (TextFieldValue) -> Unit,
    val onInsertPlaceholder: () -> Unit,
    val onSave: () -> Unit,
    val onDiscard: () -> Unit,
    val onMakeCopy: () -> Unit,
    val onDelete: () -> Unit,
    val onRestore: () -> Unit,
)

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onExit: (EditorExit) -> Unit,
) {
    val exit = viewModel.exit
    LaunchedEffect(exit) { exit?.let(onExit) }
    when (viewModel.phase) {
        EditorPhase.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {}
        }

        EditorPhase.Missing -> {
            Surface(modifier = Modifier.fillMaxSize()) {}
            LaunchedEffect(Unit) { onExit(EditorExit.Cancelled) }
        }

        EditorPhase.Ready -> {
            EditorContent(
                state =
                    EditorUiState(
                        draft = viewModel.draft,
                        categories = viewModel.categories,
                        isNew = viewModel.isNew,
                        builtIn = viewModel.builtIn,
                        showErrors = viewModel.showErrors,
                        dirty = viewModel.isDirty,
                    ),
                actions =
                    EditorActions(
                        onEdit = viewModel::edit,
                        onBodyChange = { value -> viewModel.edit { it.withBody(value) } },
                        onInsertPlaceholder = { viewModel.edit { it.insertPlaceholder(viewModel.newPlaceholderName) } },
                        onSave = viewModel::save,
                        onDiscard = viewModel::discard,
                        onMakeCopy = viewModel::makeCopy,
                        onDelete = viewModel::delete,
                        onRestore = viewModel::restoreOriginal,
                    ),
            )
        }
    }
}

@Composable
fun EditorContent(
    state: EditorUiState,
    actions: EditorActions,
) {
    var dialog by rememberSaveable { mutableStateOf<EditorDialog?>(null) }
    val requestCancel = { if (state.dirty) dialog = EditorDialog.Discard else actions.onDiscard() }
    BackHandler(onBack = requestCancel)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(
                title = stringResource(if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit),
                onNavigate = requestCancel,
                navigationIcon = DeaftlyIcons.Close,
                navigationLabel = stringResource(R.string.editor_cancel),
                trailing = {
                    if (!state.isNew) {
                        DeaftlyIconButton(DeaftlyIcons.Trash, stringResource(R.string.editor_delete), onClick = {
                            dialog =
                                EditorDialog.Delete
                        })
                    }
                    SaveButton(actions.onSave)
                },
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space20),
            ) {
                EditorFields(state, actions)
                EditorLinks(state, actions, onAsk = { dialog = it })
            }
        }
    }
    dialog?.let { EditorDialogs(it, state, actions, onClose = { dialog = null }) }
}

@Composable
private fun EditorFields(
    state: EditorUiState,
    actions: EditorActions,
) {
    val draft = state.draft
    val errors = if (state.showErrors) draft.errors else emptySet()
    LabelledTextField(
        label = stringResource(R.string.editor_name),
        value = draft.name,
        onValueChange = { value -> actions.onEdit { it.copy(name = value) } },
        error = stringResource(R.string.editor_error_name).takeIf { EditorError.NameBlank in errors },
    )
    CategoryField(state, actions, error = stringResource(R.string.editor_error_category).takeIf { EditorError.CategoryBlank in errors })
    MessageField(
        draft = draft,
        onChange = actions.onBodyChange,
        error = stringResource(R.string.editor_error_message).takeIf { EditorError.MessageBlank in errors },
    )
    ToolButton(stringResource(R.string.editor_add_placeholder), DeaftlyIcons.Plus, actions.onInsertPlaceholder)
    if (draft.placeholders.size > 1) {
        PlaceholderPicker(draft, actions)
    }
    draft.selected?.let { PlaceholderCard(draft, it, actions.onEdit) }
}

@Composable
private fun CategoryField(
    state: EditorUiState,
    actions: EditorActions,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
        LabelledTextField(
            label = stringResource(R.string.editor_category),
            value = state.draft.category,
            onValueChange = { value -> actions.onEdit { it.copy(category = value) } },
            error = error,
        )
        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
            state.categories.forEach { category ->
                CategoryChip(
                    label = category,
                    selected = category == state.draft.category.trim(),
                    onClick = { actions.onEdit { it.copy(category = category) } },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderPicker(
    draft: EditorDraft,
    actions: EditorActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
        FieldLabel(stringResource(R.string.editor_placeholders))
        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
            draft.placeholders.forEach { placeholder ->
                CategoryChip(
                    label = placeholder.key,
                    selected = placeholder.key == draft.selectedKey,
                    onClick = { actions.onEdit { it.select(placeholder.key) } },
                )
            }
        }
    }
}

@Composable
private fun EditorLinks(
    state: EditorUiState,
    actions: EditorActions,
    onAsk: (EditorDialog) -> Unit,
) {
    if (state.builtIn) {
        LinkButton(stringResource(R.string.editor_restore), onClick = { onAsk(EditorDialog.Restore) })
    }
    if (!state.isNew) {
        LinkButton(stringResource(R.string.editor_copy), onClick = actions.onMakeCopy)
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = SaveHeight),
        shape = Deaftly.shapes.pill,
        contentPadding =
            PaddingValues(
                horizontal = Deaftly.spacing.space16,
                vertical = ButtonDefaults.ContentPadding.calculateTopPadding(),
            ),
        colors = ButtonDefaults.buttonColors(containerColor = Deaftly.colors.ink, contentColor = Deaftly.colors.ground),
    ) {
        Text(stringResource(R.string.editor_save), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun EditorDialogs(
    dialog: EditorDialog,
    state: EditorUiState,
    actions: EditorActions,
    onClose: () -> Unit,
) {
    val cancel = stringResource(R.string.editor_dialog_cancel)
    when (dialog) {
        EditorDialog.Discard -> {
            val spec =
                ConfirmSpec(
                    title = stringResource(R.string.editor_discard_title),
                    text = stringResource(R.string.editor_discard_text),
                    confirm = stringResource(R.string.editor_discard_confirm),
                    dismiss = stringResource(R.string.editor_keep_editing),
                    destructive = true,
                )
            ConfirmDialog(spec, onConfirm = actions.onDiscard, onDismiss = onClose)
        }

        EditorDialog.Delete -> {
            val spec =
                ConfirmSpec(
                    title = stringResource(R.string.editor_delete_title),
                    text =
                        stringResource(
                            if (state.builtIn) R.string.editor_delete_builtin_text else R.string.editor_delete_text,
                            state.draft.name.trim(),
                        ),
                    confirm = stringResource(R.string.editor_delete_confirm),
                    dismiss = cancel,
                    destructive = true,
                )
            ConfirmDialog(spec, onConfirm = actions.onDelete, onDismiss = onClose)
        }

        EditorDialog.Restore -> {
            val spec =
                ConfirmSpec(
                    title = stringResource(R.string.editor_restore_title),
                    text = stringResource(R.string.editor_restore_text),
                    confirm = stringResource(R.string.editor_restore_confirm),
                    dismiss = cancel,
                )
            ConfirmDialog(
                spec,
                onConfirm = {
                    actions.onRestore()
                    onClose()
                },
                onDismiss = onClose,
            )
        }
    }
}
