package com.ravendarque.deaftly.ui.chat

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onEnd: () -> Unit,
    onInsertTemplate: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val composerActions = rememberComposerActions(viewModel, onInsertTemplate)
    var editingParticipants by rememberSaveable { mutableStateOf(false) }
    when (val current = state) {
        ChatUiState.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {}
        }

        is ChatUiState.Ready -> {
            if (current.layout == ChatLayout.Split) {
                SplitChat(current, rememberSplitActions(viewModel, onEnd))
            } else {
                SidesChat(
                    current,
                    viewModel,
                    composerActions,
                    onEnd,
                    onEditParticipants = { editingParticipants = true },
                )
            }
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
        }
    }
}

@Composable
private fun SidesChat(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    composerActions: ComposerActions,
    onEnd: () -> Unit,
    onEditParticipants: () -> Unit,
) {
    val conversation = current.conversation
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            ChatHeader(
                conversation = conversation,
                onEnd = onEnd,
                onEditParticipants = onEditParticipants,
                textSize = current.textSize,
                onCycleTextSize = viewModel.display::cycleTextSize,
            )
            MessageList(conversation, current.textSize, viewModel::quote, Modifier.weight(1f).fillMaxSize())
            Composer(
                participant = conversation.activeParticipant,
                draft = current.draft,
                canQuote = conversation.messages.isNotEmpty(),
                actions = composerActions,
                modifier = Modifier.padding(horizontal = Deaftly.spacing.space12),
            )
            TurnControl(
                conversation = conversation,
                onSelect = viewModel::selectTurn,
                modifier = Modifier.padding(Deaftly.spacing.space12),
            )
        }
    }
}

@Composable
private fun rememberSplitActions(
    viewModel: ChatViewModel,
    onEnd: () -> Unit,
): SplitActions =
    remember(viewModel, onEnd) {
        SplitActions(
            onChange = viewModel.drafts::setComposition,
            onSend = viewModel::send,
            onSelectTurn = viewModel::selectTurn,
            onEnd = onEnd,
        )
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
            onQuoteLatest = viewModel::quoteLatest,
            onRemoveQuote = { viewModel.drafts.setQuote(null) },
            onSend = viewModel::send,
        )
    }
