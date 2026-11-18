package com.ravendarque.deaftly.core.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

public class FakeAudioSource(
    private val chunks: Int = Int.MAX_VALUE,
    private val failWith: Exception? = null,
) : AudioSource {
    @Volatile
    public var opened: Int = 0
        private set

    @Volatile
    public var isOpen: Boolean = false
        private set

    override fun chunks(): Flow<FloatArray> =
        flow {
            failWith?.let { throw it }
            opened++
            isOpen = true
            try {
                repeat(chunks) {
                    emit(FloatArray(SPEECH_CHUNK_SAMPLES))
                    yield()
                }
            } finally {
                isOpen = false
            }
        }
}
