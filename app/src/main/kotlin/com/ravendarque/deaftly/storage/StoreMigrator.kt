package com.ravendarque.deaftly.storage

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

class UnreadableStoreException(
    message: String,
) : IllegalArgumentException(message)

interface StoreMigration {
    val from: Int

    fun migrate(document: JsonObject): JsonObject
}

class StoreMigrator(
    private val currentVersion: Int = STORE_VERSION,
    migrations: List<StoreMigration> = storeMigrations,
) {
    private val byFrom = migrations.associateBy { it.from }

    init {
        require(byFrom.size == migrations.size) { "Each version may have only one migration" }
        require(byFrom.keys.all { it in 1 until currentVersion }) { "Migrations must start below the current version" }
    }

    fun migrate(document: JsonObject): JsonObject {
        var version = readVersion(document)
        var current = document
        while (version < currentVersion) {
            val migration = byFrom[version] ?: throw UnreadableStoreException("No migration from version $version")
            version++
            current = JsonObject(migration.migrate(current) + ("version" to JsonPrimitive(version)))
        }
        return current
    }

    private fun readVersion(document: JsonObject): Int {
        val version =
            (document["version"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                ?: throw UnreadableStoreException("Store has no version")
        if (version !in 1..currentVersion) throw UnreadableStoreException("Unsupported store version $version")
        return version
    }
}
