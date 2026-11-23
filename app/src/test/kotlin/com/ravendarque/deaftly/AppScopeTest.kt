package com.ravendarque.deaftly

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppScopeTest {
    @Test
    fun aFailureInBackgroundWorkIsLoggedByTypeOnlyAndOtherWorkCarriesOn() =
        runTest {
            val scope = appScope(UnconfinedTestDispatcher(testScheduler))

            scope.launch { throw IllegalStateException("contains a chat message") }.join()
            var carriedOn = false
            scope.launch { carriedOn = true }.join()

            val logged = ShadowLog.getLogsForTag("Deaftly").map { it.msg }
            assertEquals(listOf("Background work failed: IllegalStateException"), logged)
            assertTrue(carriedOn)
        }
}
