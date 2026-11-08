package com.ravendarque.deaftly.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.ravendarque.deaftly.core.template.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

const val TEMPLATE_STORE_FILE = "templates.json"

fun createTemplateStore(
    file: File,
    builtIns: List<Template>,
    scope: CoroutineScope,
    onCorruption: (Throwable) -> Unit = ::logCorruption,
    migrator: StoreMigrator = StoreMigrator(),
): DataStore<StoreDocument> {
    val seed = StoreDocument.seeded(builtIns)
    return DataStoreFactory.create(
        serializer = TemplateStoreSerializer(seed, migrator),
        corruptionHandler =
            ReplaceFileCorruptionHandler { failure ->
                onCorruption(failure)
                seed
            },
        scope = scope,
        produceFile = { file },
    )
}

fun createTemplateRepository(
    context: Context,
    builtIns: List<Template>,
): DataStoreTemplateRepository {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    return DataStoreTemplateRepository(
        createTemplateStore(context.dataStoreFile(TEMPLATE_STORE_FILE), builtIns, scope),
        builtIns,
    )
}
