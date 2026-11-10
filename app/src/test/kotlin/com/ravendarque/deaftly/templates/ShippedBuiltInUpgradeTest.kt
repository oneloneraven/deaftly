package com.ravendarque.deaftly.templates

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.storage.DataStoreTemplateRepository
import com.ravendarque.deaftly.storage.createTemplateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ShippedBuiltInUpgradeTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val shipped = builtInTemplates(RuntimeEnvironment.getApplication().resources)

    private suspend fun upgradeV039Store(scope: CoroutineScope): List<Template> {
        val file = File(folder.root, "templates.json")
        file.writeText(checkNotNull(javaClass.getResource("/stores/v0.39-store.json")).readText())
        return DataStoreTemplateRepository(createTemplateStore(file, shipped, scope), shipped).templates.first()
    }

    @Test
    fun aV039StoreGetsTheCurrentDoctorTemplate() =
        runTest {
            val upgraded = upgradeV039Store(backgroundScope)

            val doctor = upgraded.first { it.id == BuiltInIds.DOCTOR }
            assertEquals(shipped.first { it.id == BuiltInIds.DOCTOR }, doctor)
            assertTrue("{how I communicate}" in doctor.body)
        }

    @Test
    fun aV039StoreReceivesTheBslInterpreterTemplate() =
        runTest {
            val upgraded = upgradeV039Store(backgroundScope)

            assertEquals(shipped.first { it.id == BuiltInIds.BSL_INTERPRETER }, upgraded.first { it.id == BuiltInIds.BSL_INTERPRETER })
            assertEquals(shipped.map { it.id }.toSet(), upgraded.map { it.id }.toSet())
        }
}
