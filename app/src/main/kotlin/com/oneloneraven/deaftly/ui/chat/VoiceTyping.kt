package com.oneloneraven.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.oneloneraven.deaftly.core.speech.AudioSource
import com.oneloneraven.deaftly.core.speech.AudioUnavailableException
import com.oneloneraven.deaftly.core.speech.SpeechEvent
import com.oneloneraven.deaftly.core.speech.SpeechRecogniser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

private const val VOICE_TYPING_KEY = "voice_typing"

data class VoiceTypingState(
    val on: Boolean = false,
    val listening: Boolean = false,
    val live: String = "",
    val unavailable: Boolean = false,
)

class VoiceTyping(
    private val savedState: SavedStateHandle,
    private val scope: CoroutineScope,
    private val work: CoroutineDispatcher,
    private val createRecogniser: () -> SpeechRecogniser,
    private val audio: AudioSource,
    private val onUtterance: (String) -> Unit,
) : AutoCloseable {
    private val mutableState = MutableStateFlow(VoiceTypingState(on = savedState[VOICE_TYPING_KEY] ?: false))
    val state: StateFlow<VoiceTypingState> = mutableState

    private val lock = Any()
    private var recogniser: SpeechRecogniser? = null
    private var job: Job? = null
    private var canListen = false

    @Volatile
    private var generation = 0

    fun turnOn() {
        setOn(true)
        mutableState.update { it.copy(unavailable = false) }
        reconcile()
    }

    fun turnOff() {
        stop()
        closeRecogniser()
        setOn(false)
    }

    fun update(canListen: Boolean = this.canListen) {
        if (canListen && !this.canListen) mutableState.update { it.copy(unavailable = false) }
        this.canListen = canListen
        reconcile()
    }

    override fun close() {
        stop()
        closeRecogniser()
    }

    private fun setOn(on: Boolean) {
        mutableState.update { it.copy(on = on) }
        savedState[VOICE_TYPING_KEY] = on
    }

    private fun reconcile() {
        val shouldListen = mutableState.value.on && canListen
        if (shouldListen && job == null) {
            startListening()
        } else if (!shouldListen && job != null) {
            stop()
        }
    }

    private fun startListening() {
        val id = ++generation
        mutableState.update { it.copy(listening = true, live = "") }
        job =
            scope.launch {
                try {
                    val engine = withContext(work) { obtain(id) }
                    val chunks =
                        try {
                            audio.chunks()
                        } catch (e: SecurityException) {
                            throw AudioUnavailableException("Microphone permission missing", e)
                        }
                    chunks.collect { samples ->
                        val event = withContext(work) { engine.accept(samples) }
                        if (id != generation) return@collect
                        if (event is SpeechEvent.Partial) {
                            mutableState.update { it.copy(live = event.text) }
                        } else if (event is SpeechEvent.Utterance) {
                            mutableState.update { it.copy(live = "") }
                            if (event.text.isNotBlank()) onUtterance(event.text)
                        }
                    }
                    if (id == generation) stop()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (id == generation) {
                        stop()
                        closeRecogniser()
                        setOn(false)
                        mutableState.update { it.copy(unavailable = true) }
                    }
                }
            }
    }

    private fun obtain(id: Int): SpeechRecogniser {
        synchronized(lock) { recogniser }?.let { return it }
        val created = createRecogniser()
        synchronized(lock) {
            if (id != generation) {
                created.close()
                throw CancellationException("Listening was stopped while the engine loaded")
            }
            recogniser = created
        }
        return created
    }

    fun stop() {
        generation++
        job?.cancel()
        job = null
        val live = mutableState.value.live
        mutableState.update { it.copy(listening = false, live = "") }
        if (live.isNotBlank()) {
            closeRecogniser()
            onUtterance(live)
        }
    }

    private fun closeRecogniser() {
        synchronized(lock) {
            recogniser?.close()
            recogniser = null
        }
    }
}
