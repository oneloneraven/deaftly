package com.ravendarque.deaftly.storage

import androidx.datastore.core.DataStore
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DataStoreTemplateRepository(
    private val store: DataStore<StoreDocument>,
    private val builtIns: List<Template>,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : TemplateRepository {
    init {
        require(builtIns.all { it.builtIn }) { "Built-in templates must be marked built-in" }
    }

    override val templates: Flow<List<Template>> = store.data.map { document -> document.templates.map(TemplateDto::toDomain) }

    override suspend fun add(template: Template) {
        edit { current ->
            require(current.none { it.id == template.id }) { "A template with this id already exists" }
            current + template.copy(builtIn = false)
        }
    }

    override suspend fun update(template: Template) {
        replace(template.id) { existing -> template.copy(builtIn = existing.builtIn) }
    }

    override suspend fun delete(id: String) {
        edit { current -> current.filterNot { it.id == id } }
    }

    override suspend fun duplicate(
        id: String,
        title: String,
    ): Template? {
        var copy: Template? = null
        edit { current ->
            val source = current.firstOrNull { it.id == id } ?: return@edit current
            val created = source.copy(id = newId(), title = title, pinned = false, builtIn = false)
            copy = created
            current + created
        }
        return copy
    }

    override suspend fun setPinned(
        id: String,
        pinned: Boolean,
    ) {
        replace(id) { it.copy(pinned = pinned) }
    }

    override suspend fun setCategory(
        id: String,
        category: String,
    ) {
        replace(id) { it.copy(category = category) }
    }

    override suspend fun restoreBuiltIn(id: String) {
        val default = builtIns.firstOrNull { it.id == id } ?: return
        edit { current ->
            if (current.any { it.id == id }) current.map { if (it.id == id) default else it } else current + default
        }
    }

    private suspend fun replace(
        id: String,
        change: (Template) -> Template,
    ) {
        edit { current -> current.map { if (it.id == id) change(it) else it } }
    }

    private suspend fun edit(change: (List<Template>) -> List<Template>) {
        store.updateData { document ->
            document.copy(templates = change(document.templates.map(TemplateDto::toDomain)).map(Template::toDto))
        }
    }
}
