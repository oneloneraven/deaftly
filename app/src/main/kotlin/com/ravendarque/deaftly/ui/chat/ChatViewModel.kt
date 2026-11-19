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
import com.ravendarque.deaftly.core.speech.AudioSource
import com.ravendarque.deaftly.core.speech.SpeechRecogniser
import com.ravendarque.deaftly.storage.SavedChatStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val CONVERSATION_KEY = "conversation"

data class ChatOpening(
    val messages: List<String> = emptyList(),
    val draft: String = "",
    val savedChatId: String? = null,
)

sealed interface ChatUiState {
    data object Loading : ChatUiState

    data class Ready(
        val conversation: Conversation,
        val textSize: ChatTextSize,
        val sidesSwapped: Boolean,
        val drafts: Map<ParticipantId, Draft>,
        val firstNameInChats: String? = null,
        val kept: Boolean = false,
        val voice: VoiceTypingState = VoiceTypingState(),
    ) : ChatUiState {
        val draft: Draft get() = drafts[conversation.turn] ?: Draft()
    }
}

class ChatViewModel(
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    private val opening: ChatOpening = ChatOpening(),
    private val insertions: ChatInsertions = ChatInsertions(),
    store: SavedChatStore,
    writeScope: CoroutineScope,
    createSpeechRecogniser: () -> SpeechRecogniser,
    audio: AudioSource,
    speechWork: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val conversation = MutableStateFlow(ChatCodec.decode(savedState[CONVERSATION_KEY]))
    val display = ChatDisplay(settings, viewModelScope)
    val drafts = ChatDrafts(savedState) { conversation.value?.turn }
    val keeper = ChatKeeper(store, savedState, writeScope)
    val voice =
        VoiceTyping(savedState, viewModelScope, speechWork, createSpeechRecogniser, audio) { text ->
            change { it.appendVoiceTyped(text) }
        }
    private val listeningAllowed = MutableStateFlow(false to false)

    val latestConversation: Conversation? get() = conversation.value

    val state: StateFlow<ChatUiState> =
        combine(
            combine(conversation, keeper.keptId, voice.state) { chat, id, speech -> Triple(chat, id != null, speech) },
            display.textSize,
            display.sidesSwapped,
            drafts.all,
            settings.personalNames,
        ) { (conversation, kept, speech), textSize, swapped, drafts, names ->
            if (conversation == null) {
                ChatUiState.Loading
            } else {
                val firstName = names.chatName?.takeIf { it == conversation.participantA.name }
                ChatUiState.Ready(conversation, textSize, swapped, drafts, firstName, kept, speech)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ChatUiState.Loading)

    init {
        addCloseable(voice)
        viewModelScope.launch {
            combine(
                conversation.map { it?.turn == ParticipantId.B }.distinctUntilChanged(),
                listeningAllowed,
            ) { onB, (foreground, permitted) ->
                onB && foreground && permitted
            }.collect(voice::update)
        }
        viewModelScope.launch {
            insertions.text.filterNotNull().collect {
                drafts.insertText(it)
                insertions.consume()
            }
        }
        viewModelScope.launch {
            if (conversation.value == null) {
                val reopened = opening.savedChatId?.let { keeper.open(it) }
                val started = reopened ?: opened(opening, settings.startingProfile().startConversation())
                if (conversation.compareAndSet(null, started)) {
                    savedState.saveConversation(started)
                    if (reopened == null) drafts.restore(ParticipantId.A, opening.draft)
                }
            }
        }
    }

    fun updateParticipant(participant: Participant) {
        change { it.withParticipant(participant) }
    }

    fun rememberOwnDefaults() {
        val own = conversation.value?.participantA ?: return
        viewModelScope.launch {
            val usingFirstName =
                settings.personalNames
                    .first()
                    .chatName
                    ?.let { it == own.name } == true
            val name = if (usingFirstName) settings.chatProfile.first().name else own.name
            settings.setChatProfile(ChatProfile(name, own.colour))
        }
    }

    fun useDifferentName() {
        viewModelScope.launch {
            val stored = settings.chatProfile.first().name
            change { it.withParticipant(it.participantA.copy(name = stored)) }
            settings.updatePersonalNames { it.copy(useFirstNameInChats = false) }
        }
    }

    fun selectTurn(id: ParticipantId) {
        change { it.withTurn(id) }
    }

    fun quote(
        messageId: Int,
        excerpt: String? = null,
    ) {
        val current = conversation.value ?: return
        if (messageId in current.messages.indices && !drafts.isEditing(current.turn, messageId)) {
            drafts.setQuote(current.quote(messageId, excerpt))
        }
    }

    fun deleteMessage(messageId: Int) {
        val current = conversation.value ?: return
        if (messageId !in current.messages.indices) return
        change { it.deleteMessage(messageId) }
        drafts.afterDeleting(messageId)
    }

    fun setListeningAllowed(
        foreground: Boolean,
        permitted: Boolean,
    ) {
        listeningAllowed.value = foreground to permitted
    }

    fun startEdit(messageId: Int) {
        val current = conversation.value ?: return
        val message =
            current.messages.getOrNull(messageId)?.takeIf { it.author == current.turn && !drafts.isEditing(current.turn, messageId) }
                ?: return
        voice.turnOff()
        drafts.startEdit(current.turn, message)
    }

    fun send() {
        val current = conversation.value
        val draft = current?.let { drafts.all.value[it.turn] }
        if (draft != null && !draft.composition.isBlank) {
            val spans =
                draft.composition.content
                    .trimmed()
                    .spans
            val editing = draft.editing
            change { if (editing == null) it.append(spans, draft.quote) else it.replaceMessage(editing.messageId, spans, draft.quote) }
            drafts.clear()
        }
    }

    private fun change(transform: (Conversation) -> Conversation) {
        conversation.updateAndGet { current -> current?.let(transform)?.also(savedState::saveConversation) }?.let(keeper::persist)
    }
}

private suspend fun SettingsRepository.startingProfile(): ChatProfile {
    val profile = chatProfile.first()
    val name = personalNames.first().chatName
    return if (name == null) profile else profile.copy(name = name)
}

private fun opened(
    opening: ChatOpening,
    conversation: Conversation,
): Conversation {
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

private fun SavedStateHandle.saveConversation(conversation: Conversation) {
    this[CONVERSATION_KEY] = ChatCodec.encode(conversation)
}
