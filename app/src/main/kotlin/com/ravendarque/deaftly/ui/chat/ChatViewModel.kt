package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val CONVERSATION_KEY = "conversation"
private const val TEXT_SIZE_KEY = "text_size"
private const val DRAFT_A_KEY = "draft_a"
private const val DRAFT_B_KEY = "draft_b"

sealed interface ChatUiState {
    data object Loading : ChatUiState

    data class Ready(
        val conversation: Conversation,
        val textSize: ChatTextSize,
        val drafts: Map<ParticipantId, String>,
    ) : ChatUiState {
        val draft: String get() = drafts[conversation.turn].orEmpty()
    }
}

class ChatViewModel(
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val conversation = MutableStateFlow(ChatCodec.decode(savedState[CONVERSATION_KEY]))
    private val textSize = MutableStateFlow(ChatTextSize.entries.getOrElse(savedState.get<Int>(TEXT_SIZE_KEY) ?: 0) { ChatTextSize.Normal })
    private val drafts =
        MutableStateFlow(
            mapOf(
                ParticipantId.A to savedState.get<String>(DRAFT_A_KEY).orEmpty(),
                ParticipantId.B to savedState.get<String>(DRAFT_B_KEY).orEmpty(),
            ),
        )

    val state: StateFlow<ChatUiState> =
        combine(conversation, textSize, drafts) { conversation, textSize, drafts ->
            if (conversation == null) ChatUiState.Loading else ChatUiState.Ready(conversation, textSize, drafts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ChatUiState.Loading)

    init {
        if (conversation.value == null) {
            viewModelScope.launch {
                val started = settings.chatProfile.first().startConversation()
                if (conversation.compareAndSet(null, started)) save(started)
            }
        }
    }

    fun updateParticipant(participant: Participant) {
        change { it.withParticipant(participant) }
    }

    fun rememberOwnDefaults() {
        val own = conversation.value?.participantA ?: return
        viewModelScope.launch { settings.setChatProfile(ChatProfile(own.name, own.colour)) }
    }

    fun selectTurn(id: ParticipantId) {
        change { it.withTurn(id) }
    }

    fun cycleTextSize() {
        textSize.update { it.next() }
        savedState[TEXT_SIZE_KEY] = textSize.value.ordinal
    }

    fun setDraft(text: String) {
        val turn = conversation.value?.turn ?: return
        drafts.update { it + (turn to text) }
        savedState[draftKey(turn)] = text
    }

    fun send() {
        val current = conversation.value ?: return
        val text = drafts.value[current.turn].orEmpty().trim()
        if (text.isEmpty()) return
        change { it.append(RichText.spans(text)) }
        setDraft("")
    }

    private fun change(transform: (Conversation) -> Conversation) {
        conversation.update { current -> current?.let(transform)?.also(::save) }
    }

    private fun save(conversation: Conversation) {
        savedState[CONVERSATION_KEY] = ChatCodec.encode(conversation)
    }

    private fun draftKey(id: ParticipantId) = if (id == ParticipantId.A) DRAFT_A_KEY else DRAFT_B_KEY
}
