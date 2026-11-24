package com.oneloneraven.deaftly.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oneloneraven.deaftly.core.speech.SPEECH_SAMPLE_RATE
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SpeechNativeInputValidationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val native = NativeSherpa()

    private fun create(): Long =
        native.create(context.assets, MODEL_DIR, threads = 1, trailingSilence = PAUSE_SECONDS).also { check(it != 0L) }

    @Test
    fun emptyArrayChangesNothing() {
        val handle = create()
        try {
            assertEquals(NativeSherpa.NO_CHANGE, native.accept(handle, FloatArray(0)))
        } finally {
            native.release(handle)
        }
    }

    @Test
    fun arrayOverTenSecondsIsRefused() {
        val handle = create()
        try {
            assertEquals(NativeSherpa.NO_CHANGE, native.accept(handle, FloatArray(10 * SPEECH_SAMPLE_RATE + 1)))
        } finally {
            native.release(handle)
        }
    }

    @Test
    fun releasedAndZeroHandlesChangeNothing() {
        val handle = create()
        native.release(handle)
        native.release(handle)
        assertEquals(NativeSherpa.NO_CHANGE, native.accept(handle, FloatArray(SPEECH_SAMPLE_RATE)))
        assertEquals(NativeSherpa.NO_CHANGE, native.accept(0L, FloatArray(SPEECH_SAMPLE_RATE)))
        assertEquals("", native.text(handle))
        native.release(0L)
    }
}
