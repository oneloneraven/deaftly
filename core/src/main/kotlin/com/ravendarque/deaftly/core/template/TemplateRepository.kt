package com.ravendarque.deaftly.core.template

import kotlinx.coroutines.flow.Flow

public interface TemplateRepository {
    public val templates: Flow<List<Template>>

    public suspend fun add(template: Template)

    public suspend fun update(template: Template)

    public suspend fun delete(id: String)

    public suspend fun duplicate(
        id: String,
        title: String,
    ): Template?

    public suspend fun setPinned(
        id: String,
        pinned: Boolean,
    )

    public suspend fun setCategory(
        id: String,
        category: String,
    )

    public suspend fun restoreBuiltIn(id: String)
}
