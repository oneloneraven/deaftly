package com.ravendarque.deaftly.ocr

import android.Manifest
import android.content.pm.PackageManager
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
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class NoNetworkTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun appRequestsNoInternetPermission() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.INTERNET))
    }

    @Test
    fun socketsAreRefusedAndRecognitionSendsNothing() {
        assertFailsWith<Exception> { Socket().use { it.connect(InetSocketAddress("93.184.216.34", 80), CONNECT_TIMEOUT_MS) } }
        val uid = Process.myUid()
        val before = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        PaddleOcrNcnnRecogniser(context, PaddleOcrConfig(threads = 2)).use { recogniser ->
            FixtureSet.load().take(SAMPLE).forEach { runBlocking { recogniser.recognise(it.frame) } }
        }
        val after = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        assertEquals(before, after)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 2000
        const val SAMPLE = 3
    }
}
