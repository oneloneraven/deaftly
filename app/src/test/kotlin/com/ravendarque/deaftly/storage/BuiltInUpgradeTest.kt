package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.template.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuiltInUpgradeTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "templates.json") }
    private val extra = Template(id = "builtin-extra", title = "Extra", category = "Everyday", body = "Extra {name}", builtIn = true)

    private fun received(vararg shipped: Template) = shipped.associate { it.id to it.toDto().contentFingerprint() }

    private suspend fun writeStore(document: StoreDocument) {
        val out = ByteArrayOutputStream()
        TemplateStoreSerializer(document).writeTo(document, out)
        file.writeBytes(out.toByteArray())
    }

    private fun store(
        shipped: List<Template>,
        scope: CoroutineScope,
    ) = createTemplateStore(file, shipped, scope)

    private fun repository(
        shipped: List<Template>,
        scope: CoroutineScope,
    ) = DataStoreTemplateRepository(store(shipped, scope), shipped)

    private fun document(
        templates: List<Template>,
        received: Map<String, String>,
    ) = StoreDocument(STORE_VERSION, templates.map(Template::toDto), SettingsDto(), received)

    @Test
    fun aNewBuiltInIsAddedToAnExistingStore() =
        runTest {
            writeStore(document(builtIns, received(*builtIns.toTypedArray())))

            val repository = repository(builtIns + extra, backgroundScope)

            assertEquals(builtIns + extra, repository.templates.first())
            assertEquals(0, repository.hiddenBuiltInCount.first())
        }

    @Test
    fun anUneditedBuiltInIsUpdatedAndKeepsItsPinnedState() =
        runTest {
            val unpinned = greeting.copy(pinned = false)
            writeStore(document(listOf(unpinned, menu), received(greeting, menu)))
            val reworded = greeting.copy(body = "Hello! I'm {name}. Please {how to reply}.")

            val repository = repository(listOf(reworded, menu), backgroundScope)

            assertEquals(listOf(reworded.copy(pinned = false), menu), repository.templates.first())
        }

    @Test
    fun anEditedBuiltInIsLeftAlone() =
        runTest {
            val edited = greeting.copy(title = "My hello")
            writeStore(document(listOf(edited, menu), received(greeting, menu)))
            val reworded = greeting.copy(body = "Hello! I'm {name}. Please {how to reply}.")

            val repository = repository(listOf(reworded, menu), backgroundScope)

            assertEquals(listOf(edited, menu), repository.templates.first())
        }

    @Test
    fun aDeletedBuiltInStaysDeletedEvenWhenItsShippedContentChanges() =
        runTest {
            writeStore(document(listOf(greeting), received(greeting, menu)))
            val reworded = menu.copy(body = "May I have {item}?")

            val repository = repository(listOf(greeting, reworded), backgroundScope)

            assertEquals(listOf(greeting), repository.templates.first())
            assertEquals(1, repository.hiddenBuiltInCount.first())
        }

    @Test
    fun aBuiltInThatWasNeverReceivedIsNotCountedAsHiddenAndRestoreLeavesItOut() =
        runTest {
            writeStore(document(builtIns, received(*builtIns.toTypedArray())))
            val shipped = builtIns + extra
            val firstRun = repository(shipped, backgroundScope)
            firstRun.delete(extra.id)

            assertEquals(1, firstRun.hiddenBuiltInCount.first())

            firstRun.restoreHiddenBuiltIns()

            assertEquals(shipped, firstRun.templates.first())
            assertEquals(0, firstRun.hiddenBuiltInCount.first())
        }

    @Test
    fun aStoreRestoredFromAnOlderReleaseGetsTheNewBuiltIns() =
        runTest {
            writeStore(document(builtIns, received(greeting)))

            val repository = repository(builtIns + extra, backgroundScope)

            assertEquals(builtIns + extra, repository.templates.first())
        }

    @Test
    fun reconcilingTwiceChangesNothingMore() {
        val shipped = builtIns + extra
        val once = document(builtIns, received(*builtIns.toTypedArray())).reconcileBuiltIns(shipped)

        assertEquals(once, once.reconcileBuiltIns(shipped))
        assertEquals(received(*shipped.toTypedArray()), once.builtInsReceived)
    }

    @Test
    fun aBuiltInRestoredAfterEditingIsUpdatedByALaterRelease() =
        runTest {
            val firstRun = Job()
            val first = repository(builtIns, CoroutineScope(firstRun))
            first.update(greeting.copy(title = "Edited"))
            first.restoreBuiltIn(greeting.id)
            firstRun.cancelAndJoin()
            val reworded = greeting.copy(body = "Hello! I'm {name}. Please {how to reply}.")

            val upgraded = repository(listOf(reworded, menu), backgroundScope)

            assertEquals(listOf(reworded, menu), upgraded.templates.first())
        }

    @Test
    fun aDeletedBuiltInRestoredIsUpdatedByALaterRelease() =
        runTest {
            val firstRun = Job()
            val first = repository(builtIns, CoroutineScope(firstRun))
            first.delete(greeting.id)
            first.restoreHiddenBuiltIns()
            firstRun.cancelAndJoin()
            val reworded = greeting.copy(body = "Hello! I'm {name}. Please {how to reply}.")

            val upgraded = repository(listOf(reworded, menu), backgroundScope)

            assertEquals(listOf(menu, reworded), upgraded.templates.first())
        }

    @Test
    fun aFreshStoreRecordsEveryShippedBuiltInAsReceived() {
        assertEquals(received(*builtIns.toTypedArray()), StoreDocument.seeded(builtIns).builtInsReceived)
    }

    private val lenient = Json { ignoreUnknownKeys = true }
    private val v039 = checkNotNull(javaClass.getResource("/stores/v0.39-store.json")).readText()
    private val v039Templates =
        Json
            .parseToJsonElement(v039)
            .jsonObject
            .getValue("templates")
            .jsonArray

    private fun v039Shipped(): List<Template> = lenient.decodeFromString<StoreDocument>(v039).templates.map(TemplateDto::toDomain)

    private fun v039With(templates: List<kotlinx.serialization.json.JsonElement>) =
        JsonObject(Json.parseToJsonElement(v039).jsonObject + ("templates" to JsonArray(templates))).toString()

    @Test
    fun theFrozenVersionOneFingerprintsMatchTheShippedContentOfV039() {
        val shipped = v039Shipped()

        assertEquals(9, shipped.size)
        assertEquals(shipped.associate { it.id to it.toDto().contentFingerprint() }, RecordBuiltInsReceived.shippedAtVersion1)
    }

    @Test
    fun aV039StoreMigratesWithItsNineBuiltInsReceivedAndUnedited() =
        runTest {
            file.writeText(v039)
            val shipped = v039Shipped()
            val newer = shipped.map { if (it.id == "builtin.doctor") it.copy(body = "New doctor wording") else it } + extra

            val repository = repository(newer, backgroundScope)
            val result = repository.templates.first()

            assertEquals(10, result.size)
            assertEquals("New doctor wording", result.first { it.id == "builtin.doctor" }.body)
            assertEquals(extra, result.last())
            assertEquals(0, repository.hiddenBuiltInCount.first())
        }

    @Test
    fun aV039StoreWithADeletedAndAnEditedBuiltInKeepsBoth() =
        runTest {
            val templates =
                v039Templates.filterNot {
                    it.jsonObject
                        .getValue("id")
                        .jsonPrimitive.content == "builtin.pharmacy"
                }
            val withEdit =
                templates.map {
                    val template = it.jsonObject
                    if (template.getValue("id").jsonPrimitive.content == "builtin.doctor") {
                        JsonObject(template + ("title" to JsonPrimitive("My doctor")))
                    } else {
                        template
                    }
                }
            file.writeText(v039With(withEdit))
            val shipped = v039Shipped()
            val newer = shipped.map { if (it.id in setOf("builtin.doctor", "builtin.pharmacy")) it.copy(body = "Changed") else it }

            val repository = repository(newer, backgroundScope)
            val result = repository.templates.first()

            assertEquals("My doctor", result.first { it.id == "builtin.doctor" }.title)
            assertEquals(shipped.first { it.id == "builtin.doctor" }.body, result.first { it.id == "builtin.doctor" }.body)
            assertTrue(result.none { it.id == "builtin.pharmacy" })
            assertEquals(1, repository.hiddenBuiltInCount.first())

            repository.restoreHiddenBuiltIns()

            assertEquals(
                "Changed",
                repository.templates
                    .first()
                    .first { it.id == "builtin.pharmacy" }
                    .body,
            )
        }

    @Test
    fun theMigratedV039StoreIsWrittenAtTheCurrentVersion() =
        runTest {
            file.writeText(v039)
            val repository = repository(v039Shipped(), backgroundScope)

            repository.setPinned("builtin.paying", true)

            val written = Json.parseToJsonElement(file.readText()).jsonObject
            assertEquals(STORE_VERSION.toString(), written.getValue("version").jsonPrimitive.content)
            assertEquals(9, written.getValue("builtInsReceived").jsonObject.size)
            assertFalse(written.getValue("builtInsReceived").jsonObject.isEmpty())
        }
}
