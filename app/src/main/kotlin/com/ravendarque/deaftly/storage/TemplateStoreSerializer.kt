package com.ravendarque.deaftly.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.InputStream
import java.io.OutputStream

private val storeJson =
    Json {
        encodeDefaults = true
        explicitNulls = false
    }

class TemplateStoreSerializer(
    override val defaultValue: StoreDocument,
    private val migrator: StoreMigrator = StoreMigrator(),
    private val json: Json = storeJson,
) : Serializer<StoreDocument> {
    override suspend fun readFrom(input: InputStream): StoreDocument =
        try {
            val root =
                json.parseToJsonElement(input.readBytes().decodeToString()) as? JsonObject
                    ?: throw UnreadableStoreException("Store is not an object")
            json.decodeFromJsonElement<StoreDocument>(migrator.migrate(root)).also { document ->
                document.templates.forEach(TemplateDto::toDomain)
            }
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Unreadable template store", e)
        }

    override suspend fun writeTo(
        t: StoreDocument,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
