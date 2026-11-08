package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun repository(scope: CoroutineScope) =
        DataStoreSettingsRepository(createTemplateStore(File(folder.root, "templates.json"), builtIns, scope))

    @Test
    fun hasNoChosenSchemeUntilOneIsSet() =
        runTest {
            assertNull(repository(backgroundScope).showScheme.first())
        }

    @Test
    fun theChosenSchemeSurvivesAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).setShowScheme(ShowScheme.YellowOnBlack)
            firstRun.cancelAndJoin()

            assertEquals(ShowScheme.YellowOnBlack, repository(backgroundScope).showScheme.first())
        }
}
