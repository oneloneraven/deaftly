package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
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
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.safeDrawingPadding()) {
                    ChatHeader(
                        conversation = current.conversation,
                        onEnd = onEnd,
                        onEditParticipants = { editingParticipants = true },
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Deaftly.colors.inkMuted,
                        )
                    }
                }
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
