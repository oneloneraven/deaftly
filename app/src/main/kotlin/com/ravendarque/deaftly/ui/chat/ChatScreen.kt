package com.ravendarque.deaftly.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.editor.ConfirmDialog
import com.ravendarque.deaftly.ui.editor.ConfirmSpec
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onEnd: () -> Unit,
    onInsertTemplate: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ChatUiState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
        is ChatUiState.Ready -> ChatContent(current, viewModel, onEnd, onInsertTemplate)
    }
}

@Composable
private fun ChatContent(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    onEnd: () -> Unit,
    onInsertTemplate: () -> Unit,
) {
    var editingParticipants by rememberSaveable { mutableStateOf(false) }
    var confirmingEnd by rememberSaveable { mutableStateOf(false) }
    val requestEnd = { if (current.conversation.messages.isEmpty()) onEnd() else confirmingEnd = true }
    BackHandler(onBack = requestEnd)
    SidesChat(
        current,
        viewModel,
        rememberComposerActions(viewModel, onInsertTemplate),
        ChatHeaderActions(
            onEnd = requestEnd,
            onEditParticipants = { editingParticipants = true },
            onCycleTextSize = viewModel.display::cycleTextSize,
            onSwitchSides = viewModel.display::switchSides,
        ),
    )
    if (editingParticipants) {
        ParticipantsSheet(
            conversation = current.conversation,
            onChange = viewModel::updateParticipant,
            onDone = {
                viewModel.rememberOwnDefaults()
                editingParticipants = false
            },
        )
    }
    if (confirmingEnd) {
        ConfirmDialog(
            spec =
                ConfirmSpec(
                    title = stringResource(R.string.chat_end_title),
                    text = stringResource(R.string.chat_end_text),
                    confirm = stringResource(R.string.chat_end_confirm),
                    dismiss = stringResource(R.string.chat_end_keep),
                    destructive = true,
                ),
            onConfirm = {
                confirmingEnd = false
                onEnd()
            },
            onDismiss = { confirmingEnd = false },
        )
    }
}

@Composable
private fun SidesChat(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    composerActions: ComposerActions,
    headerActions: ChatHeaderActions,
) {
    val conversation = current.conversation
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            ChatHeader(current.textSize, headerActions)
            MessageList(
                conversation,
                current.textSize,
                current.sidesSwapped,
                MessageActions(onQuote = { viewModel.quote(it) }, onDelete = viewModel::deleteMessage),
                Modifier.weight(1f).fillMaxSize(),
            )
            Composer(
                participant = conversation.activeParticipant,
                hasSent = conversation.messages.any { it.author == conversation.turn },
                draft = current.draft,
                textSize = current.textSize,
                actions = composerActions,
                modifier = Modifier.padding(horizontal = Deaftly.spacing.space12),
            )
            TurnControl(
                conversation = conversation,
                sidesSwapped = current.sidesSwapped,
                onSelect = viewModel::selectTurn,
                modifier = Modifier.padding(Deaftly.spacing.space12),
            )
        }
    }
}

@Composable
private fun rememberComposerActions(
    viewModel: ChatViewModel,
    onInsertTemplate: () -> Unit,
): ComposerActions =
    remember(viewModel, onInsertTemplate) {
        ComposerActions(
            onInsertTemplate = onInsertTemplate,
            onChange = viewModel.drafts::setComposition,
            onRemoveQuote = { viewModel.drafts.setQuote(null) },
            onSend = viewModel::send,
        )
    }
