package com.oneloneraven.deaftly.ui.chat

import com.oneloneraven.deaftly.core.speech.AudioSource
import com.oneloneraven.deaftly.core.speech.FakeAudioSource
import com.oneloneraven.deaftly.core.speech.FakeSpeechRecogniser
import com.oneloneraven.deaftly.core.speech.SpeechRecogniser

internal fun silentRecogniser(): SpeechRecogniser = FakeSpeechRecogniser(emptyList())

internal fun silentAudio(): AudioSource = FakeAudioSource(chunks = 0)

internal class HangingAudio(
    private val chunks: Int,
) : AudioSource {
    var opened = 0
        private set
    var isOpen = false
        private set

    override fun chunks() =
        kotlinx.coroutines.flow.flow {
            opened++
            isOpen = true
            try {
                repeat(chunks) { emit(FloatArray(com.oneloneraven.deaftly.core.speech.SPEECH_CHUNK_SAMPLES)) }
                kotlinx.coroutines.awaitCancellation()
            } finally {
                isOpen = false
            }
        }
}
