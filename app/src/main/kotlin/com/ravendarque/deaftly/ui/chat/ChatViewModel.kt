package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val CONVERSATION_KEY = "conversation"

data class ChatOpening(
    val messages: List<String> = emptyList(),
    val draft: String = "",
)

sealed interface ChatUiState {
    data object Loading : ChatUiState

    data class Ready(
        val conversation: Conversation,
        val textSize: ChatTextSize,
        val sidesSwapped: Boolean,
        val drafts: Map<ParticipantId, Draft>,
    ) : ChatUiState {
        val draft: Draft get() = drafts[conversation.turn] ?: Draft()
    }
}

class ChatViewModel(
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    private val opening: ChatOpening = ChatOpening(),
    private val insertions: ChatInsertions = ChatInsertions(),
) : ViewModel() {
    private val conversation = MutableStateFlow(ChatCodec.decode(savedState[CONVERSATION_KEY]))
    val drafts = ChatDrafts(savedState) { conversation.value?.turn }

    val state: StateFlow<ChatUiState> =
        combine(conversation, settings.chatTextSize, settings.chatSidesSwapped, drafts.all) { conversation, textSize, swapped, drafts ->
            if (conversation == null) ChatUiState.Loading else ChatUiState.Ready(conversation, textSize, swapped, drafts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ChatUiState.Loading)

    init {
        viewModelScope.launch {
            insertions.text.filterNotNull().collect {
                drafts.insertText(it)
                insertions.consume()
            }
        }
        viewModelScope.launch {
            if (conversation.value == null) {
                val started = opened(settings.chatProfile.first().startConversation())
                if (conversation.compareAndSet(null, started)) {
                    save(started)
                    drafts.restore(ParticipantId.A, opening.draft)
                }
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

    fun cycleTextSize() {
        viewModelScope.launch { settings.setChatTextSize(settings.chatTextSize.first().next()) }
    }

    fun switchSides() {
        viewModelScope.launch { settings.setChatSidesSwapped(!settings.chatSidesSwapped.first()) }
    }

    fun selectTurn(id: ParticipantId) {
        change { it.withTurn(id) }
    }

    fun quote(
        messageId: Int,
        excerpt: String? = null,
    ) {
        val current = conversation.value ?: return
        if (messageId !in current.messages.indices) return
        drafts.setQuote(current.quote(messageId, excerpt))
    }

    fun quoteLatest() {
        val current = conversation.value ?: return
        val latest = current.messages.lastOrNull { it.author != current.turn } ?: current.messages.lastOrNull() ?: return
        quote(latest.id)
    }

    fun send() {
        val current = conversation.value
        val draft = current?.let { drafts.all.value[it.turn] }
        if (draft != null && !draft.composition.isBlank) {
            val spans =
                draft.composition.content
                    .trimmed()
                    .spans
            change { it.append(spans, draft.quote) }
            drafts.clear()
        }
    }

    private fun opened(conversation: Conversation): Conversation {
        val sent = opening.messages.map { it.trim() }.filter { it.isNotEmpty() }
        return if (sent.isEmpty()) {
            conversation
        } else {
            sent
                .fold(
                    conversation,
                ) { chat, text -> chat.append(RichText.spans(text)) }
                .withTurn(ParticipantId.B)
        }
    }

    private fun change(transform: (Conversation) -> Conversation) {
        conversation.update { current -> current?.let(transform)?.also(::save) }
    }

    private fun save(conversation: Conversation) {
        savedState[CONVERSATION_KEY] = ChatCodec.encode(conversation)
    }
}
