package com.ravendarque.deaftly.storage

import androidx.datastore.core.CorruptionException
import com.ravendarque.deaftly.core.template.Join
import com.ravendarque.deaftly.core.template.PersonalField
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    fun theJoinChoiceSurvivesSavingAndLoading() =
        runTest {
            val document = StoreDocument(STORE_VERSION, builtIns.map { it.toDto() }, SettingsDto())

            val written = write(document)
            val restored = read(written).templates.map { it.toDomain() }

            assertTrue("\"join\":\"or\"" in written)
            assertEquals(Join.Or, restored.first { it.id == greeting.id }.placeholder("how to reply").join)
            assertEquals(Join.And, restored.first { it.id == menu.id }.placeholder("item").join)
        }

    @Test
    fun thePersonalFieldAPlaceholderFillsWithSurvivesSavingAndLoading() =
        runTest {
            PersonalField.entries.forEach { field ->
                val filled =
                    greeting.copy(
                        placeholders = greeting.placeholders + ("name" to greeting.placeholder("name").copy(fillsWith = field)),
                    )
                val document = StoreDocument(STORE_VERSION, listOf(filled, menu).map { it.toDto() }, SettingsDto())

                val restored = read(write(document)).templates.map { it.toDomain() }

                assertEquals(field, restored.first { it.id == greeting.id }.placeholder("name").fillsWith)
                assertNull(restored.first { it.id == greeting.id }.placeholder("how to reply").fillsWith)
                assertNull(restored.first { it.id == menu.id }.placeholder("item").fillsWith)
            }
        }

    @Test
    fun writesAVersionedDocumentWithTemplatesAndSettings() =
        runTest {
            val root = Json.parseToJsonElement(write(seed)).jsonObject

            assertEquals(2, root.getValue("version").jsonPrimitive.int)
            assertEquals(builtIns.size, root.getValue("templates").jsonArray.size)
            assertEquals(
                JsonObject(
                    mapOf(
                        "firstRunDone" to JsonPrimitive(false),
                        "chatSidesSwapped" to JsonPrimitive(false),
                        "listenSignFacesThem" to JsonPrimitive(true),
                        "firstName" to JsonPrimitive(""),
                        "lastName" to JsonPrimitive(""),
                        "legalNameDiffers" to JsonPrimitive(false),
                        "legalName" to JsonPrimitive(""),
                        "useFirstNameInChats" to JsonPrimitive(false),
                    ),
                ),
                root.getValue("settings"),
            )
        }

    @Test
    fun roundTripsTheShowScheme() =
        runTest {
            val document = seed.copy(settings = SettingsDto(ShowSchemeDto.YellowOnBlack))

            val written = write(document)

            assertEquals(
                "yellow-on-black",
                Json
                    .parseToJsonElement(written)
                    .jsonObject
                    .getValue("settings")
                    .jsonObject
                    .getValue("showScheme")
                    .jsonPrimitive.content,
            )
            assertEquals(document.settings, read(written).settings)
        }

    @Test
    fun settingsSavedByEarlierVersionsHaveNoThemeMode() =
        runTest {
            val earlier =
                """{"version":1,"templates":[],"settings":{"showScheme":"black-on-white","chatName":"Alex","firstRunDone":true}}"""

            assertNull(read(earlier).settings.themeMode)
        }

    @Test
    fun theThemeModeIsStoredByName() =
        runTest {
            val written = write(seed.copy(settings = SettingsDto(themeMode = ThemeModeDto.Dark)))

            assertEquals(
                "dark",
                Json
                    .parseToJsonElement(written)
                    .jsonObject
                    .getValue("settings")
                    .jsonObject
                    .getValue("themeMode")
                    .jsonPrimitive.content,
            )
            assertEquals(ThemeModeDto.Dark, read(written).settings.themeMode)
        }

    @Test
    fun readsSettingsWithoutAShowScheme() =
        runTest {
            assertEquals(SettingsDto(), read("""{"version":1,"templates":[],"settings":{}}""").settings)
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
            assertFailsWith<CorruptionException> { read("""{"version":3,"templates":[],"settings":{}}""") }
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
            val document = seed.copy(settings = SettingsDto(chatName = "Alex", chatColour = ParticipantColourDto.Orange))

            val settings =
                Json
                    .parseToJsonElement(write(document))
                    .jsonObject
                    .getValue("settings")
                    .jsonObject

            assertEquals("Alex", settings.getValue("chatName").jsonPrimitive.content)
            assertEquals("orange", settings.getValue("chatColour").jsonPrimitive.content)
            assertEquals(document.settings, read(write(document)).settings)
        }

    @Test
    fun readsSettingsWithoutAChatProfile() =
        runTest {
            assertEquals(SettingsDto(), read("""{"version":1,"templates":[],"settings":{}}""").settings)
        }

    @Test
    fun roundTripsTheChatTextSizeAndSwappedSides() =
        runTest {
            val document = seed.copy(settings = SettingsDto(chatTextSize = ChatTextSizeDto.Large, chatSidesSwapped = true))

            val settings =
                Json
                    .parseToJsonElement(write(document))
                    .jsonObject
                    .getValue("settings")
                    .jsonObject

            assertEquals("large", settings.getValue("chatTextSize").jsonPrimitive.content)
            assertEquals(true, settings.getValue("chatSidesSwapped").jsonPrimitive.boolean)
            assertEquals(document.settings, read(write(document)).settings)
        }

    @Test
    fun anOlderDocumentWithoutTheChatDisplaySettingsStillLoadsWithDefaults() =
        runTest {
            val settings =
                read("""{"version":1,"templates":[],"settings":{"chatName":"Alex","chatColour":"green","firstRunDone":true}}""").settings

            assertEquals(null, settings.chatTextSize)
            assertEquals(false, settings.chatSidesSwapped)
            assertEquals("Alex", settings.chatName)
        }

    @Test
    fun readsAStoredSplitLayoutSettingAndIgnoresIt() =
        runTest {
            val document =
                read("""{"version":1,"templates":[],"settings":{"chatName":"Alex","chatLayout":"split","firstRunDone":true}}""")

            assertEquals(SettingsDto(chatName = "Alex", firstRunDone = true), document.settings)
        }

    @Test
    fun aStoredLayoutIsDroppedWhenTheStoreIsNextWritten() =
        runTest {
            val document = read("""{"version":1,"templates":[],"settings":{"chatLayout":"sides"}}""")

            val settings =
                Json
                    .parseToJsonElement(write(document))
                    .jsonObject
                    .getValue("settings")
                    .jsonObject

            assertEquals(null, settings["chatLayout"])
        }

    @Test
    fun readsRetiredColourNamesAsTheirNewColours() =
        runTest {
            mapOf(
                "accent" to ParticipantColourDto.Indigo,
                "person-b" to ParticipantColourDto.Orange,
                "rose" to ParticipantColourDto.Red,
                "teal" to ParticipantColourDto.Green,
                "forest" to ParticipantColourDto.Green,
                "berry" to ParticipantColourDto.Green,
            ).forEach { (stored, expected) ->
                val settings = read("""{"version":1,"templates":[],"settings":{"chatColour":"$stored"}}""").settings

                assertEquals(expected, settings.chatColour, stored)
            }
        }

    @Test
    fun writesEachColourAsItsOwnName() =
        runTest {
            ParticipantColourDto.entries.forEach { colour ->
                val document = seed.copy(settings = SettingsDto(chatColour = colour))

                val settings =
                    Json
                        .parseToJsonElement(write(document))
                        .jsonObject
                        .getValue("settings")
                        .jsonObject

                assertEquals(colour.name.lowercase(), settings.getValue("chatColour").jsonPrimitive.content)
            }
        }
}
