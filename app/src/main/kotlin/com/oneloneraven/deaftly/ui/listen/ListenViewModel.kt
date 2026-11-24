package com.oneloneraven.deaftly.ui.listen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneloneraven.deaftly.core.settings.ChatTextSize
import com.oneloneraven.deaftly.core.settings.SettingsRepository
import com.oneloneraven.deaftly.core.speech.AudioSource
import com.oneloneraven.deaftly.core.speech.SpeechRecogniser
import com.oneloneraven.deaftly.navigation.Route
import com.oneloneraven.deaftly.ui.chat.VoiceTyping
import com.oneloneraven.deaftly.ui.chat.VoiceTypingState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val HEARD_KEY = "heard"
private const val STARTED_KEY = "listen_started"

data class ListenUiState(
    val heard: List<String> = emptyList(),
    val voice: VoiceTypingState = VoiceTypingState(),
    val textSize: ChatTextSize = ChatTextSize.Default,
    val signFacesThem: Boolean = true,
)

class ListenViewModel(
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    createSpeechRecogniser: () -> SpeechRecogniser,
    audio: AudioSource,
    speechWork: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val heard = savedState.getStateFlow(HEARD_KEY, ArrayList<String>())

    val voice =
        VoiceTyping(savedState, viewModelScope, speechWork, createSpeechRecogniser, audio) { text ->
            savedState[HEARD_KEY] = ArrayList(heard.value + text.trim())
        }

    val state: StateFlow<ListenUiState> =
        combine(heard, voice.state, settings.chatTextSize, settings.listenSignFacesThem) { heard, voice, size, facesThem ->
            ListenUiState(heard, voice, size, facesThem)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ListenUiState())

    init {
        addCloseable(voice)
        if (savedState.get<Boolean>(STARTED_KEY) == null) {
            savedState[STARTED_KEY] = true
            voice.turnOn()
        }
    }

    val isEmpty: Boolean get() =
        heard.value.isEmpty() &&
            voice.state.value.live
                .isBlank()

    fun setListeningAllowed(allowed: Boolean) {
        voice.update(canListen = allowed)
    }

    fun toggleListening() {
        if (voice.state.value.on) voice.turnOff() else voice.turnOn()
    }

    fun flipSign() {
        viewModelScope.launch { settings.setListenSignFacesThem(!settings.listenSignFacesThem.first()) }
    }

    fun continueAsChat(): Route.Chat {
        val live =
            voice.state.value.live
                .trim()
        val route = Route.Chat(heard = if (live.isEmpty()) heard.value else heard.value + live)
        voice.close()
        return route
    }
}
