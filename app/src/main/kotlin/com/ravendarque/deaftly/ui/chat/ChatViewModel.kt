package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val A_NAME_KEY = "a_name"
private const val A_COLOUR_KEY = "a_colour"
private const val B_NAME_KEY = "b_name"
private const val B_COLOUR_KEY = "b_colour"

sealed interface ChatUiState {
    data object Loading : ChatUiState

    data class Ready(
        val conversation: Conversation,
    ) : ChatUiState
}

class ChatViewModel(
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val conversation = MutableStateFlow(restore())

    val state: StateFlow<ChatUiState> =
        conversation
            .map { it?.let(ChatUiState::Ready) ?: ChatUiState.Loading }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ChatUiState.Loading)

    init {
        if (conversation.value == null) {
            viewModelScope.launch {
                val started = settings.chatProfile.first().startConversation()
                if (conversation.compareAndSet(null, started)) save(started)
            }
        }
    }

    fun updateParticipant(participant: Participant) {
        conversation.update { current -> current?.withParticipant(participant)?.also(::save) }
    }

    fun rememberOwnDefaults() {
        val own = conversation.value?.participantA ?: return
        viewModelScope.launch { settings.setChatProfile(ChatProfile(own.name, own.colour)) }
    }

    private fun save(conversation: Conversation) {
        savedState[A_NAME_KEY] = conversation.participantA.name
        savedState[A_COLOUR_KEY] = conversation.participantA.colour.name
        savedState[B_NAME_KEY] = conversation.participantB.name
        savedState[B_COLOUR_KEY] = conversation.participantB.colour.name
    }

    private fun restore(): Conversation? {
        val a = savedState.get<String>(A_COLOUR_KEY)?.let(::colourOrNull)
        val b = savedState.get<String>(B_COLOUR_KEY)?.let(::colourOrNull)
        return if (a == null || b == null || a == b) {
            null
        } else {
            Conversation(
                Participant(ParticipantId.A, a, savedState[A_NAME_KEY]),
                Participant(ParticipantId.B, b, savedState[B_NAME_KEY]),
            )
        }
    }

    private fun colourOrNull(name: String): ColourRole? = ColourRole.entries.firstOrNull { it.name == name }
}
