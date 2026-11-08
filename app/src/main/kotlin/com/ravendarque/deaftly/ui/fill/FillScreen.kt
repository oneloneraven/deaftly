package com.ravendarque.deaftly.ui.fill

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun FillScreen(
    templateId: String,
    viewModel: FillViewModel,
    onNavigate: (Route) -> Unit,
    onBack: () -> Unit,
    onInsert: ((String) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        FillUiState.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {}
        }

        FillUiState.Missing -> {
            Surface(modifier = Modifier.fillMaxSize()) {}
            LaunchedEffect(Unit) { onBack() }
        }

        is FillUiState.Ready -> {
            val actions =
                remember(viewModel, templateId, current.text, current.active?.placeholder?.key, onNavigate, onBack, onInsert) {
                    FillActions(
                        onBack = onBack,
                        onEdit = { onNavigate(Route.Editor(templateId)) },
                        onShow = { if (onInsert != null) onInsert(current.text) else onNavigate(Route.Show(current.text)) },
                        onActivate = viewModel::activate,
                        onDismiss = viewModel::dismiss,
                        onChoose = viewModel::choose,
                        onRemove = viewModel::remove,
                        onScan = { current.active?.let { onNavigate(Route.Ocr(templateId, it.placeholder.key)) } },
                    )
                }
            FillContent(current, actions, insertIntoChat = onInsert != null)
        }
    }
}

@Composable
fun FillContent(
    state: FillUiState.Ready,
    actions: FillActions,
    insertIntoChat: Boolean = false,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(
                title = state.title,
                onNavigate = actions.onBack,
                trailing = {
                    DeaftlyIconButton(DeaftlyIcons.Edit, stringResource(R.string.fill_edit), onClick = actions.onEdit)
                },
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
            ) {
                MessagePreview(
                    parts = state.parts,
                    text = state.text,
                    activeKey = state.active?.placeholder?.key,
                    onActivate = actions.onActivate,
                    onRemove = actions.onRemove,
                )
                Text(
                    text = stringResource(R.string.fill_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Deaftly.colors.inkMuted,
                    modifier = Modifier.padding(start = Deaftly.spacing.space4, top = Deaftly.spacing.space10),
                )
            }
            PrimaryButton(
                text = stringResource(if (insertIntoChat) R.string.fill_insert else R.string.fill_show),
                onClick = actions.onShow,
                enabled = state.canShow,
                icon = if (insertIntoChat) DeaftlyIcons.Template else DeaftlyIcons.Expand,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Deaftly.spacing.space20,
                            top = Deaftly.spacing.space14,
                            end = Deaftly.spacing.space20,
                            bottom = Deaftly.spacing.space24,
                        ),
            )
        }
    }
    state.active?.let { FillSheet(it, actions.onChoose, actions.onScan, actions.onDismiss) }
}
