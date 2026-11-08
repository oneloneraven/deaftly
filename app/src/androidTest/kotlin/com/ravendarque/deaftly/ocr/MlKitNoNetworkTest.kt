package com.ravendarque.deaftly.ocr

import android.Manifest
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ravendarque.deaftly.ocr.bench.FixtureSet
import com.ravendarque.deaftly.ocr.mlkit.MlKitTextRecogniser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MlKitNoNetworkTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun mergedManifestHasNoNetworkPermissions() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()
        Log.i(TAG, "requested permissions: $requested")
        assertFalse(requested.contains(Manifest.permission.INTERNET))
        assertFalse(requested.contains(Manifest.permission.ACCESS_NETWORK_STATE))
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.INTERNET))
    }

    @Test
    fun recognisingAndWaitingSendsNothingAndDoesNotFail() {
        val uid = Process.myUid()
        val before = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        var recognisedBlocks = 0
        MlKitTextRecogniser().use { recogniser ->
            FixtureSet.load().take(SAMPLE).forEach { recognisedBlocks += runBlocking { recogniser.recognise(it.frame) }.blocks.size }
            Thread.sleep(SETTLE_MS)
            FixtureSet.load().take(SAMPLE).forEach { recognisedBlocks += runBlocking { recogniser.recognise(it.frame) }.blocks.size }
        }
        Thread.sleep(SETTLE_MS)
        val after = TrafficStats.getUidTxBytes(uid) to TrafficStats.getUidRxBytes(uid)
        Log.i(TAG, "TrafficStats uid tx/rx before=$before after=$after blocks=$recognisedBlocks")
        logDataFiles()
        assertTrue(recognisedBlocks > 0)
        assertEquals(before, after)
    }

    private fun logDataFiles() {
        val root = context.dataDir
        root
            .walkTopDown()
            .filter {
                it.isFile &&
                    it.relativeTo(root).path.let { path ->
                        path.startsWith("databases") || path.startsWith("shared_prefs") ||
                            path.startsWith("no_backup")
                    }
            }.forEach { Log.i(TAG, "data file ${it.relativeTo(root).path} ${it.length()} bytes") }
    }

    private companion object {
        const val TAG = "MlKitNoNetworkTest"
        const val SAMPLE = 3
        const val SETTLE_MS = 15_000L
    }
}
