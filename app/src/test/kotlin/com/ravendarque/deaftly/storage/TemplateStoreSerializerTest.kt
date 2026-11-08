package com.ravendarque.deaftly.storage

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TemplateStoreSerializerTest {
    private val seed = StoreDocument.seeded(builtIns)
    private val serializer = TemplateStoreSerializer(seed)

    private suspend fun read(json: String) = serializer.readFrom(json.byteInputStream())

    private suspend fun write(document: StoreDocument): String =
        ByteArrayOutputStream().also { serializer.writeTo(document, it) }.toString(Charsets.UTF_8)

    @Test
    fun roundTripsTemplatesAndSettings() =
        runTest {
            val custom = greeting.copy(id = "mine", builtIn = false, body = "Quote \" and {{braces}} and é 😀")
            val document = StoreDocument(STORE_VERSION, (builtIns + custom).map { it.toDto() }, SettingsDto())

            val restored = read(write(document))

            assertEquals(document.templates, restored.templates)
            assertEquals(document.version, restored.version)
            assertEquals(builtIns + custom, restored.templates.map { it.toDomain() })
        }

    @Test
    fun writesAVersionedDocumentWithTemplatesAndSettings() =
        runTest {
            val root = Json.parseToJsonElement(write(seed)).jsonObject

            assertEquals(1, root.getValue("version").jsonPrimitive.int)
            assertEquals(builtIns.size, root.getValue("templates").jsonArray.size)
            assertEquals(JsonObject(emptyMap()), root.getValue("settings"))
        }

    @Test
    fun readsDocumentWithoutOptionalFields() =
        runTest {
            val minimal =
                """{"version":1,"templates":[{"id":"a","title":"T","category":"C","body":"B"}],"settings":{}}"""

            val template = read(minimal).templates.single().toDomain()

            assertEquals("a", template.id)
            assertTrue(template.placeholders.isEmpty())
        }

    @Test
    fun unknownFutureVersionIsUnreadable() =
        runTest {
            assertFailsWith<CorruptionException> { read("""{"version":2,"templates":[],"settings":{}}""") }
        }

    @Test
    fun garbageIsUnreadable() =
        runTest {
            assertFailsWith<CorruptionException> { read("\u0000\u0001 not json {") }
            assertFailsWith<CorruptionException> { serializer.readFrom(byteArrayOf(-1, -2, 0, 12).inputStream()) }
            assertFailsWith<CorruptionException> { read("") }
            assertFailsWith<CorruptionException> { read("[]") }
        }

    @Test
    fun structurallyInvalidDocumentIsUnreadable() =
        runTest {
            assertFailsWith<CorruptionException> { read("""{"version":1,"templates":"nope","settings":{}}""") }
            assertFailsWith<CorruptionException> {
                read(
                    """{"version":1,"templates":[{"id":"a","title":"T","category":"C","body":"B",""" +
                        """"placeholders":[{"key":"k","label":"L","type":"telepathy"}]}],"settings":{}}""",
                )
            }
        }

    @Test
    fun migratesOlderDocumentsBeforeDecoding() =
        runTest {
            val noOp =
                object : StoreMigration {
                    override val from = 1

                    override fun migrate(document: JsonObject): JsonObject = document
                }
            val migrating = TemplateStoreSerializer(seed, StoreMigrator(currentVersion = 2, migrations = listOf(noOp)))

            val migrated = migrating.readFrom("""{"version":1,"templates":[],"settings":{}}""".byteInputStream())

            assertEquals(2, migrated.version)
        }

    @Test
    fun migrationCanRewriteTheDocumentShape() =
        runTest {
            val renamesTitle =
                object : StoreMigration {
                    override val from = 1

                    override fun migrate(document: JsonObject): JsonObject {
                        val templates =
                            document.getValue("templates").jsonArray.map { element ->
                                val old = element.jsonObject
                                JsonObject(old - "name" + ("title" to old.getValue("name")))
                            }
                        return JsonObject(document + ("templates" to JsonArray(templates)))
                    }
                }
            val migrating = TemplateStoreSerializer(seed, StoreMigrator(2, listOf(renamesTitle)))
            val v1 = """{"version":1,"templates":[{"id":"a","name":"Old","category":"C","body":"B"}],"settings":{}}"""

            val migrated = migrating.readFrom(v1.byteInputStream())

            assertEquals("Old", migrated.templates.single().title)
            assertEquals(2, migrated.version)
        }

    @Test
    fun roundTripsTheChatProfile() =
        runTest {
            val document = seed.copy(settings = SettingsDto(chatName = "Alex", chatColour = ParticipantColourDto.PersonB))

            val settings =
                Json
                    .parseToJsonElement(write(document))
                    .jsonObject
                    .getValue("settings")
                    .jsonObject

            assertEquals("Alex", settings.getValue("chatName").jsonPrimitive.content)
            assertEquals("person-b", settings.getValue("chatColour").jsonPrimitive.content)
            assertEquals(document.settings, read(write(document)).settings)
        }

    @Test
    fun readsSettingsWithoutAChatProfile() =
        runTest {
            assertEquals(SettingsDto(), read("""{"version":1,"templates":[],"settings":{}}""").settings)
        }
}
