package com.ravendarque.deaftly.storage

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StoreMigratorTest {
    private fun migration(
        from: Int,
        step: String,
    ) = object : StoreMigration {
        override val from = from

        override fun migrate(document: JsonObject): JsonObject = JsonObject(document + (step to JsonPrimitive(true)))
    }

    private fun document(version: Int?) = JsonObject(if (version == null) emptyMap() else mapOf("version" to JsonPrimitive(version)))

    @Test
    fun currentVersionPassesThroughUnchanged() {
        val input = document(1)

        assertEquals(input, StoreMigrator().migrate(input))
    }

    @Test
    fun exampleMigrationFromVersionOneRunsAndStampsTheNewVersion() {
        val migrator = StoreMigrator(currentVersion = 2, migrations = listOf(migration(from = 1, step = "ran1")))

        val result = migrator.migrate(document(1))

        assertEquals(2, result.getValue("version").jsonPrimitive.int)
        assertEquals(JsonPrimitive(true), result["ran1"])
    }

    @Test
    fun chainsMigrationsInOrder() {
        val migrator =
            StoreMigrator(currentVersion = 3, migrations = listOf(migration(2, "ran2"), migration(1, "ran1")))

        val result = migrator.migrate(document(1))

        assertEquals(3, result.getValue("version").jsonPrimitive.int)
        assertEquals(setOf("version", "ran1", "ran2"), result.keys)
    }

    @Test
    fun onlyRunsMigrationsNewerThanTheStoredVersion() {
        val migrator =
            StoreMigrator(currentVersion = 3, migrations = listOf(migration(1, "ran1"), migration(2, "ran2")))

        assertEquals(setOf("version", "ran2"), migrator.migrate(document(2)).keys)
    }

    @Test
    fun futureVersionIsUnreadable() {
        assertFailsWith<UnreadableStoreException> { StoreMigrator().migrate(document(2)) }
    }

    @Test
    fun missingOrNonPositiveVersionIsUnreadable() {
        assertFailsWith<UnreadableStoreException> { StoreMigrator().migrate(document(null)) }
        assertFailsWith<UnreadableStoreException> { StoreMigrator().migrate(document(0)) }
        assertFailsWith<UnreadableStoreException> { StoreMigrator().migrate(JsonObject(mapOf("version" to JsonPrimitive("1")))) }
    }

    @Test
    fun gapInTheChainIsUnreadable() {
        val migrator = StoreMigrator(currentVersion = 3, migrations = listOf(migration(2, "ran2")))

        assertFailsWith<UnreadableStoreException> { migrator.migrate(document(1)) }
    }

    @Test
    fun rejectsInvalidMigrationSets() {
        assertFailsWith<IllegalArgumentException> { StoreMigrator(2, listOf(migration(1, "a"), migration(1, "b"))) }
        assertFailsWith<IllegalArgumentException> { StoreMigrator(2, listOf(migration(2, "a"))) }
    }
}
