package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.speech.AudioUnavailableException
import com.ravendarque.deaftly.core.speech.FakeAudioSource
import com.ravendarque.deaftly.core.speech.FakeSpeechRecogniser
import com.ravendarque.deaftly.core.speech.SpeechEvent
import com.ravendarque.deaftly.core.speech.SpeechRecogniser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceTypingTest {
    private val utterances = mutableListOf<String>()
    private val created = mutableListOf<FakeSpeechRecogniser>()

    private fun TestScope.voiceTyping(
        script: List<SpeechEvent?> = emptyList(),
        audio: com.ravendarque.deaftly.core.speech.AudioSource = HangingAudio(script.size),
        savedState: SavedStateHandle = SavedStateHandle(),
        create: () -> SpeechRecogniser = { FakeSpeechRecogniser(script).also(created::add) },
    ): VoiceTyping {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return VoiceTyping(savedState, CoroutineScope(dispatcher), dispatcher, create, audio) { utterances += it }
    }

    @Test
    fun pauseSendsAnUtterance() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Partial("yeah"), SpeechEvent.Utterance("Yeah")))

            voice.turnOn()
            voice.update(true)
            runCurrent()

            assertEquals(listOf("Yeah"), utterances)
            assertEquals("", voice.state.value.live)
        }

    @Test
    fun partialsShowAsLiveText() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Partial("yeah")))

            voice.turnOn()
            voice.update(true)
            runCurrent()

            assertEquals("yeah", voice.state.value.live)
            assertTrue(utterances.isEmpty())
        }

    @Test
    fun shortUtterancesAreKept() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Utterance("Tuesday")))

            voice.turnOn()
            voice.update(true)
            runCurrent()

            assertEquals(listOf("Tuesday"), utterances)
        }

    @Test
    fun blankUtterancesAreDropped() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Utterance("  ")))

            voice.turnOn()
            voice.update(true)
            runCurrent()

            assertTrue(utterances.isEmpty())
        }

    @Test
    fun turnAwayMidUtteranceCommitsAsB() =
        runTest {
            val audio = HangingAudio(1)
            val voice = voiceTyping(listOf(SpeechEvent.Partial("I was going to")), audio)
            voice.turnOn()
            voice.update(true)
            runCurrent()

            voice.update(false)
            runCurrent()

            assertEquals(listOf("I was going to"), utterances)
            assertFalse(voice.state.value.listening)
            assertTrue(voice.state.value.on)
            assertEquals("", voice.state.value.live)
            assertFalse(audio.isOpen)
        }

    @Test
    fun stopBeforeEndCommitsLiveText() =
        runTest {
            val audio = HangingAudio(1)
            val voice = voiceTyping(listOf(SpeechEvent.Partial("Could I just")), audio)
            voice.turnOn()
            voice.update(true)
            runCurrent()

            voice.stop()
            runCurrent()

            assertEquals(listOf("Could I just"), utterances)
            assertTrue(voice.state.value.on)
            assertFalse(audio.isOpen)
        }

    @Test
    fun stoppingTwiceSendsTheLiveTextOnce() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Partial("Could I just")))
            voice.turnOn()
            voice.update(true)
            runCurrent()

            voice.stop()
            voice.update(false)
            voice.turnOff()
            runCurrent()

            assertEquals(listOf("Could I just"), utterances)
        }

    @Test
    fun turningOffClosesTheEngine() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Partial("a")))
            voice.turnOn()
            voice.update(true)
            runCurrent()

            voice.turnOff()
            runCurrent()

            assertTrue(created.single().isClosed)
            assertFalse(voice.state.value.on)

            voice.turnOn()
            runCurrent()

            assertEquals(2, created.size)
            assertFalse(created.last().isClosed)
        }

    @Test
    fun failedStartTurnsOffAndFlagsFailure() =
        runTest {
            val voice = voiceTyping(audio = FakeAudioSource(failWith = AudioUnavailableException()))

            voice.turnOn()
            voice.update(true)
            runCurrent()

            assertFalse(voice.state.value.on)
            assertTrue(voice.state.value.unavailable)
            assertFalse(voice.state.value.listening)
            assertTrue(created.single().isClosed)
        }

    @Test
    fun aRecogniserThatFailsToLoadTurnsOffAndFlagsFailure() =
        runTest {
            val voice = voiceTyping(create = { error("model missing") })

            voice.turnOn()
            voice.update(true)
            runCurrent()

            assertFalse(voice.state.value.on)
            assertTrue(voice.state.value.unavailable)
        }

    @Test
    fun turningOnAgainClearsTheFailure() =
        runTest {
            val voice = voiceTyping(create = { error("model missing") })
            voice.turnOn()
            voice.update(true)
            runCurrent()

            voice.turnOn()

            assertFalse(voice.state.value.unavailable)
            assertTrue(voice.state.value.on)
        }

    @Test
    fun theFailureClearsWhenListeningCanStartAgain() =
        runTest {
            val voice = voiceTyping(create = { error("model missing") })
            voice.turnOn()
            voice.update(true)
            runCurrent()

            voice.update(false)
            assertTrue(voice.state.value.unavailable)
            voice.update(true)

            assertFalse(voice.state.value.unavailable)
            assertFalse(voice.state.value.on)
        }

    @Test
    fun restoredOnStateWaitsForResume() =
        runTest {
            val audio = HangingAudio(0)
            val voice = voiceTyping(audio = audio, savedState = SavedStateHandle(mapOf("voice_typing" to true)))
            runCurrent()

            assertTrue(voice.state.value.on)
            assertFalse(voice.state.value.listening)
            assertEquals(0, audio.opened)

            voice.update(true)
            runCurrent()

            assertEquals(1, audio.opened)
            assertTrue(voice.state.value.listening)
        }

    @Test
    fun notListeningWhileOff() =
        runTest {
            val audio = HangingAudio(0)
            val voice = voiceTyping(audio = audio)

            voice.update(true)
            runCurrent()

            assertEquals(0, audio.opened)
            assertFalse(voice.state.value.listening)
        }

    @Test
    fun onIsSavedAndNothingElse() =
        runTest {
            val saved = SavedStateHandle()
            val voice = voiceTyping(savedState = saved)

            voice.turnOn()
            voice.update(true)

            assertEquals(setOf("voice_typing"), saved.keys())
            assertEquals(true, saved.get<Boolean>("voice_typing"))
        }

    @Test
    fun aPartialDoesNotReappearAfterStopping() =
        runTest {
            val voice = voiceTyping(listOf(SpeechEvent.Partial("one"), SpeechEvent.Partial("one two")), HangingAudio(2))
            voice.turnOn()
            voice.update(true)
            voice.stop()
            runCurrent()

            assertEquals("", voice.state.value.live)
            assertTrue(utterances.isEmpty())
        }
}
