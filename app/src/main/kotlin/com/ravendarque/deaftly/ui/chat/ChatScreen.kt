package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onEnd: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingParticipants by rememberSaveable { mutableStateOf(false) }
    when (val current = state) {
        ChatUiState.Loading -> {
            Surface(modifier = Modifier.fillMaxSize()) {}
        }

        is ChatUiState.Ready -> {
            val conversation = current.conversation
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.safeDrawingPadding()) {
                    ChatHeader(
                        conversation = conversation,
                        onEnd = onEnd,
                        onEditParticipants = { editingParticipants = true },
                        textSize = current.textSize,
                        onCycleTextSize = viewModel::cycleTextSize,
                    )
                    MessageList(conversation, current.textSize, Modifier.weight(1f).fillMaxSize())
                    Composer(
                        participant = conversation.activeParticipant,
                        draft = current.draft,
                        onDraftChange = viewModel::setDraft,
                        onSend = viewModel::send,
                        modifier = Modifier.padding(horizontal = Deaftly.spacing.space12),
                    )
                    TurnControl(
                        conversation = conversation,
                        onSelect = viewModel::selectTurn,
                        modifier =
                            Modifier.padding(
                                start = Deaftly.spacing.space12,
                                top = Deaftly.spacing.space12,
                                end = Deaftly.spacing.space12,
                                bottom = Deaftly.spacing.space12,
                            ),
                    )
                }
            }
            if (editingParticipants) {
                ParticipantsSheet(
                    conversation = conversation,
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
