package com.ravendarque.deaftly.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateStoreCorruptionTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val reported = mutableListOf<Throwable>()

    private fun repository(
        scope: CoroutineScope,
        file: File,
    ) = DataStoreTemplateRepository(
        createTemplateStore(file, builtIns, scope, onCorruption = { reported += it }),
        builtIns,
    )

    @Test
    fun garbageBytesFallBackToBuiltInTemplatesAndTheFileIsRepaired() =
        runTest {
            val file = folder.newFile("templates.json").apply { writeBytes(byteArrayOf(0, -1, 127, 55, -80, 9)) }
            val repository = repository(backgroundScope, file)

            assertEquals(builtIns, repository.templates.first())
            assertEquals(1, reported.size)

            repository.setPinned(menu.id, true)
            assertEquals(
                true,
                repository.templates
                    .first()
                    .first { it.id == menu.id }
                    .pinned,
            )
            assertTrue(file.readText().startsWith("{\"version\":2"))
        }

    @Test
    fun truncatedJsonFallsBackToBuiltInTemplates() =
        runTest {
            val file = folder.newFile("templates.json").apply { writeText("""{"version":1,"templates":[{"id":"a","tit""") }

            val repository = repository(backgroundScope, file)

            assertEquals(builtIns, repository.templates.first())
            assertEquals(1, reported.size)
        }

    @Test
    fun futureVersionFallsBackToBuiltInTemplates() =
        runTest {
            val file = folder.newFile("templates.json").apply { writeText("""{"version":99,"templates":[],"settings":{}}""") }

            val repository = repository(backgroundScope, file)

            assertEquals(builtIns, repository.templates.first())
            assertEquals(1, reported.size)
        }

    @Test
    fun firstRunSeedsBuiltInTemplatesWithoutReportingAFailure() =
        runTest {
            val file = File(folder.root, "datastore/templates.json")

            val repository = repository(backgroundScope, file)

            assertEquals(builtIns, repository.templates.first())
            assertTrue(reported.isEmpty())
        }

    @Test
    fun reportedFailureNamesTypesButNeverContent() =
        runTest {
            val secret = "MY-PRIVATE-TEMPLATE-TEXT"
            val file = folder.newFile("templates.json").apply { writeText("""{"version":1,"templates":"$secret""") }

            repository(backgroundScope, file).templates.first()

            val description = describeFailure(reported.single())
            assertTrue(description.startsWith("CorruptionException"))
            assertFalse(secret in description)
        }
}
