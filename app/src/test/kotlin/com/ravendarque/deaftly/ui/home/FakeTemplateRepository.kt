package com.ravendarque.deaftly.ui.home

import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTemplateRepository(
    initial: List<Template>,
    private val builtIns: List<Template> = emptyList(),
) : TemplateRepository {
    private val current = MutableStateFlow(initial)

    override val hiddenBuiltInCount: Flow<Int> = current.map { now -> builtIns.count { builtIn -> now.none { it.id == builtIn.id } } }

    override val templates: Flow<List<Template>> = current

    val snapshot: List<Template> get() = current.value

    override suspend fun add(template: Template) {
        current.value += template
    }

    override suspend fun update(template: Template) {
        change(template.id) { template }
    }

    override suspend fun delete(id: String) {
        current.value = current.value.filterNot { it.id == id }
    }

    override suspend fun duplicate(
        id: String,
        title: String,
    ): Template? = null

    override suspend fun setPinned(
        id: String,
        pinned: Boolean,
    ) {
        change(id) { it.copy(pinned = pinned) }
    }

    override suspend fun setCategory(
        id: String,
        category: String,
    ) {
        change(id) { it.copy(category = category) }
    }

    override suspend fun restoreBuiltIn(id: String) = Unit

    override suspend fun restoreHiddenBuiltIns() {
        current.value += builtIns.filter { builtIn -> current.value.none { it.id == builtIn.id } }
    }

    private fun change(
        id: String,
        transform: (Template) -> Template,
    ) {
        current.value = current.value.map { if (it.id == id) transform(it) else it }
    }
}
