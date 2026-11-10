package com.ravendarque.deaftly.storage

import androidx.datastore.core.DataMigration
import com.ravendarque.deaftly.core.template.Template

fun StoreDocument.reconcileBuiltIns(builtIns: List<Template>): StoreDocument {
    val shipped = builtIns.map(Template::toDto)
    var templates = this.templates
    val received = builtInsReceived.toMutableMap()
    shipped.forEach { builtIn ->
        val fingerprint = builtIn.contentFingerprint()
        val recorded = received[builtIn.id]
        val stored = templates.firstOrNull { it.id == builtIn.id }
        if (recorded == null) {
            if (stored == null) templates = templates + builtIn
            received[builtIn.id] = fingerprint
        } else if (stored != null && recorded != fingerprint) {
            val current = stored.contentFingerprint()
            if (current == recorded) {
                templates = templates.map { if (it.id == builtIn.id) builtIn.copy(pinned = it.pinned) else it }
            }
            if (current == recorded || current == fingerprint) received[builtIn.id] = fingerprint
        }
    }
    val unchanged = templates == this.templates && received == builtInsReceived
    return if (unchanged) this else copy(templates = templates, builtInsReceived = received)
}

fun StoreDocument.hiddenBuiltIns(builtIns: List<Template>): List<String> =
    builtIns.map { it.id }.filter { id -> id in builtInsReceived && templates.none { it.id == id } }

fun StoreDocument.restoring(defaults: List<Template>): StoreDocument {
    val restored = defaults.map(Template::toDto).associateBy { it.id }
    val replaced = templates.map { restored[it.id] ?: it }
    val added = restored.values.filter { default -> templates.none { it.id == default.id } }
    return copy(
        templates = replaced + added,
        builtInsReceived = builtInsReceived + restored.mapValues { it.value.contentFingerprint() },
    )
}

class BuiltInReconciliation(
    private val builtIns: List<Template>,
) : DataMigration<StoreDocument> {
    override suspend fun shouldMigrate(currentData: StoreDocument): Boolean = currentData.reconcileBuiltIns(builtIns) != currentData

    override suspend fun migrate(currentData: StoreDocument): StoreDocument = currentData.reconcileBuiltIns(builtIns)

    override suspend fun cleanUp() = Unit
}
