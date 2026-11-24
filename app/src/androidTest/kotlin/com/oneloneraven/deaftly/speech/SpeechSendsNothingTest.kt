package com.oneloneraven.deaftly.speech

import android.net.TrafficStats
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

// Backs "Speech never leaves your phone": transcription runs entirely on the device.
@RunWith(AndroidJUnit4::class)
class SpeechSendsNothingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun transcribingSendsAndReceivesNothing() {
        val uid = Process.myUid()
        val before = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        SherpaSpeechRecogniser(context).use { recogniser ->
            SpeechFixtures.feed(recogniser, SpeechFixtures.samples(testContext))
        }
        val after = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        assertEquals(before, after)
    }
}
