package com.ravendarque.deaftly.core.speech

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeSpeechRecogniserTest {
    private val chunk = FloatArray(SPEECH_CHUNK_SAMPLES)

    @Test
    fun returnsScriptedEventsInOrderThenNull() {
        val fake = FakeSpeechRecogniser(listOf(SpeechEvent.Partial("hel"), null, SpeechEvent.Utterance("hello")))
        assertEquals(SpeechEvent.Partial("hel"), fake.accept(chunk))
        assertNull(fake.accept(chunk))
        assertEquals(SpeechEvent.Utterance("hello"), fake.accept(chunk))
        assertNull(fake.accept(chunk))
        assertEquals(4, fake.accepted)
    }

    @Test
    fun refusesSamplesAfterClose() {
        val fake = FakeSpeechRecogniser(emptyList())
        assertFalse(fake.isClosed)
        fake.close()
        assertTrue(fake.isClosed)
        assertFailsWith<IllegalStateException> { fake.accept(chunk) }
    }

    @Test
    fun cancellingCollectionClosesTheSource() =
        runBlocking {
            val source = FakeAudioSource()
            val job = launch(Dispatchers.Default) { source.chunks().collect { } }
            while (!source.isOpen) yield()
            assertEquals(1, source.opened)
            job.cancelAndJoin()
            assertFalse(source.isOpen)
        }

    @Test
    fun emitsChunksOfTheAgreedSizeUntilExhausted() =
        runBlocking {
            val received = mutableListOf<FloatArray>()
            FakeAudioSource(chunks = 3).chunks().collect { received += it }
            assertEquals(listOf(SPEECH_CHUNK_SAMPLES, SPEECH_CHUNK_SAMPLES, SPEECH_CHUNK_SAMPLES), received.map { it.size })
        }

    @Test
    fun failingSourceThrowsAudioUnavailable() =
        runBlocking {
            val source = FakeAudioSource(failWith = AudioUnavailableException("busy"))
            assertFailsWith<AudioUnavailableException> { source.chunks().collect { } }
            assertFalse(source.isOpen)
        }
}
