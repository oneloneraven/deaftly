package com.ravendarque.deaftly.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.editor.ConfirmDialog
import com.ravendarque.deaftly.ui.editor.ConfirmSpec
import com.ravendarque.deaftly.ui.permissions.PermissionAccess
import com.ravendarque.deaftly.ui.permissions.RuntimePermission
import com.ravendarque.deaftly.ui.permissions.rememberMicrophonePermission
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onEnd: () -> Unit,
    onInsertTemplate: () -> Unit,
    microphone: RuntimePermission = rememberMicrophonePermission(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ChatUiState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}
        is ChatUiState.Ready -> ChatContent(current, viewModel, microphone, onEnd, onInsertTemplate)
    }
}

@Composable
private fun ChatContent(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    microphone: RuntimePermission,
    onEnd: () -> Unit,
    onInsertTemplate: () -> Unit,
) {
    var editingParticipants by rememberSaveable { mutableStateOf(false) }
    var confirmingEnd by rememberSaveable { mutableStateOf(false) }
    val requestEnd = {
        viewModel.voice.stop()
        when {
            current.kept -> {
                onEnd()
            }

            viewModel.latestConversation?.messages.isNullOrEmpty() -> {
                onEnd()
            }

            else -> {
                confirmingEnd = true
            }
        }
    }
    BackHandler(onBack = requestEnd)
    SidesChat(
        current,
        viewModel,
        rememberComposerActions(viewModel, onInsertTemplate),
        microphone,
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
            firstNameInChats = current.firstNameInChats,
            onUseDifferentName = viewModel::useDifferentName,
            onChange = viewModel::updateParticipant,
            onDone = {
                viewModel.rememberOwnDefaults()
                editingParticipants = false
            },
        )
    }
    if (confirmingEnd) {
        KeepChatDialog(
            conversation = viewModel.latestConversation ?: current.conversation,
            keeper = viewModel.keeper,
            onEnd = {
                confirmingEnd = false
                onEnd()
            },
            onKeepChatting = {
                confirmingEnd = false
                viewModel.voice.update()
            },
        )
    }
}

@Composable
private fun SidesChat(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    baseActions: ComposerActions,
    microphone: RuntimePermission,
    headerActions: ChatHeaderActions,
) {
    val notice = remember { ChatNotice() }
    val voiceControls = rememberVoiceControls(current, viewModel, microphone, notice)
    val composerActions = baseActions.copy(onToggleVoice = voiceControls.toggle)
    val voicePermission = voiceControls.permissionPrompt
    val conversation = current.conversation
    val hearingPersonsTurn = conversation.turn == ParticipantId.B
    val tail = remember { BubbleTailState() }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            ChatHeader(current.textSize, headerActions)
            MessageList(
                conversation,
                current.textSize,
                current.sidesSwapped,
                MessageActions(
                    onQuote = { viewModel.quote(it) },
                    onDelete = viewModel::deleteMessage,
                    onEdit = viewModel::startEdit,
                    onNotYours = { notice.show(R.string.chat_edit_not_yours) },
                ),
                Modifier.weight(1f).fillMaxSize(),
                liveText = current.voice.live,
                editable = { it.author == conversation.turn },
            )
            Box(contentAlignment = Alignment.Center) {
                Composer(
                    participant = conversation.activeParticipant,
                    hasSent = conversation.messages.any { it.author == conversation.turn },
                    draft = current.draft,
                    textSize = current.textSize,
                    actions = composerActions,
                    modifier = Modifier.padding(horizontal = Deaftly.spacing.space12),
                    tail = tail,
                    voice = current.voice.takeIf { hearingPersonsTurn },
                    voicePermission = voicePermission.takeIf { hearingPersonsTurn },
                )
                TransientNotice(notice, Modifier.padding(horizontal = Deaftly.spacing.space24))
            }
            TurnControl(
                conversation = conversation,
                sidesSwapped = current.sidesSwapped,
                onSelect = viewModel::selectTurn,
                modifier = Modifier.padding(Deaftly.spacing.space12),
                tail = tail,
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
            onToggleVoice = {},
            onCancelEdit = viewModel.drafts::cancelEdit,
        )
    }

private class VoiceControls(
    val toggle: () -> Unit,
    val permissionPrompt: VoicePermissionPrompt?,
)

@Composable
private fun rememberVoiceControls(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    microphone: RuntimePermission,
    notice: ChatNotice,
): VoiceControls {
    val asking = rememberSaveable { mutableStateOf(false) }
    VoiceEffects(current, viewModel, microphone.access == PermissionAccess.Granted, asking)
    val toggle = {
        val voice = viewModel.voice
        when {
            current.voice.unavailable -> {
                notice.show(R.string.chat_voice_unavailable)
            }

            microphone.access == PermissionAccess.Granted -> {
                if (current.voice.on) voice.turnOff() else voice.turnOn()
            }

            microphone.access == PermissionAccess.DeniedForGood -> {
                asking.value = !asking.value
            }

            else -> {
                asking.value = true
                microphone.request()
            }
        }
    }
    val prompt =
        if (asking.value && microphone.access != PermissionAccess.Granted) {
            VoicePermissionPrompt(microphone.access == PermissionAccess.DeniedForGood, microphone.request, microphone.openSettings)
        } else {
            null
        }
    return VoiceControls(toggle, prompt)
}

@Composable
private fun VoiceEffects(
    current: ChatUiState.Ready,
    viewModel: ChatViewModel,
    granted: Boolean,
    asking: MutableState<Boolean>,
) {
    val voiceOn = current.voice.on
    val editing = current.draft.editing != null
    val hearingPersonsTurn = current.conversation.turn == ParticipantId.B
    LifecycleResumeEffect(granted) {
        viewModel.setListeningAllowed(foreground = true, permitted = granted)
        onPauseOrDispose { viewModel.setListeningAllowed(foreground = false, permitted = granted) }
    }
    LaunchedEffect(granted) {
        if (!granted && voiceOn) viewModel.voice.turnOff()
    }
    LaunchedEffect(granted, asking.value, editing) {
        if (granted && asking.value && !editing) {
            asking.value = false
            viewModel.voice.turnOn()
        }
    }
    LaunchedEffect(hearingPersonsTurn, editing) {
        if (!hearingPersonsTurn || editing) asking.value = false
    }
}
