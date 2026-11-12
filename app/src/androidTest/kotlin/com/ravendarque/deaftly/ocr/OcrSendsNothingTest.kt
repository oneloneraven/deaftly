package com.ravendarque.deaftly.ocr

import android.net.TrafficStats
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrConfig
import com.ravendarque.deaftly.ocr.paddle.PaddleOcrNcnnRecogniser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

// Backs "Pictures never leave your phone": text recognition runs entirely on the device.
@RunWith(AndroidJUnit4::class)
class OcrSendsNothingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun recognisingTextSendsAndReceivesNothing() {
        val uid = Process.myUid()
        val before = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        PaddleOcrNcnnRecogniser(context, PaddleOcrConfig(threads = 2)).use { recogniser ->
            FixtureSet.load().take(SAMPLE).forEach { runBlocking { recogniser.recognise(it.frame) } }
        }
        val after = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        assertEquals(before, after)
    }

    private companion object {
        const val SAMPLE = 3
    }
}
