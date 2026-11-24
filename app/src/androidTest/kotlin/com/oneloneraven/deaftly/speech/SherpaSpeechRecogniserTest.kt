package com.oneloneraven.deaftly.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oneloneraven.deaftly.core.speech.SPEECH_SAMPLE_RATE
import com.oneloneraven.deaftly.core.speech.SpeechEvent
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SherpaSpeechRecogniserTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun transcribesTheFixture() {
        val fixture = SpeechFixtures.samples(testContext)
        val silence = FloatArray(TRAILING_SILENCE_SAMPLES)
        SherpaSpeechRecogniser(context).use { recogniser ->
            val start = System.nanoTime()
            val events = SpeechFixtures.feed(recogniser, fixture + silence)
            val seconds = (System.nanoTime() - start) / NANOS_PER_SECOND
            val audioSeconds = (fixture.size + silence.size).toDouble() / SPEECH_SAMPLE_RATE
            println("speech.realTimeFactor=${seconds / audioSeconds}")
            val utterances = events.filterIsInstance<SpeechEvent.Utterance>()
            assertEquals(1, utterances.size)
            val wer = SpeechFixtures.wordErrorRate(SpeechFixtures.transcript(testContext), utterances.single().text)
            println("speech.wordErrorRate=$wer")
            assertTrue(wer <= MAX_WORD_ERROR_RATE, "word error rate $wer")
        }
    }

    @Test
    fun silenceGivesNoUtterance() {
        SherpaSpeechRecogniser(context).use { recogniser ->
            val events = SpeechFixtures.feed(recogniser, FloatArray(5 * SPEECH_SAMPLE_RATE))
            assertTrue(events.none { it is SpeechEvent.Utterance })
            assertTrue(events.filterIsInstance<SpeechEvent.Partial>().all { it.text.isBlank() })
        }
    }

    @Test
    fun missingModelThrowsInsteadOfCrashing() {
        assertFailsWith<IllegalStateException> {
            SherpaSpeechRecogniser(context, SherpaConfig(modelDir = "speech-missing"))
        }
    }

    @Test
    fun acceptAfterCloseThrows() {
        val recogniser = SherpaSpeechRecogniser(context)
        recogniser.close()
        recogniser.close()
        assertFailsWith<IllegalStateException> { recogniser.accept(FloatArray(1)) }
    }

    private companion object {
        const val TRAILING_SILENCE_SAMPLES = 40_000
        const val MAX_WORD_ERROR_RATE = 0.15
        const val NANOS_PER_SECOND = 1e9
    }
}
