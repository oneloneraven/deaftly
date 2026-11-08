package com.ravendarque.deaftly.ui.editor

import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.ui.fill.checkIn
import com.ravendarque.deaftly.ui.fill.orderFood
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository

val builtInOrderFood = orderFood.copy(builtIn = true, pinned = true)

val myNote =
    Template(
        id = "my-note",
        title = "My note",
        category = "Everyday",
        body = "Hello {name}, I need {thing}.",
        pinned = true,
        placeholders =
            mapOf(
                "name" to Placeholder("name", "Your name", PlaceholderType.Typing),
                "thing" to
                    Placeholder(
                        "thing",
                        "What do you need?",
                        PlaceholderType.Choices,
                        listOf("water", "a chair"),
                        repeatable = true,
                    ),
            ),
    )

val editorTemplates = listOf(checkIn, builtInOrderFood, myNote)

val editorStrings = EditorStrings(newPlaceholderName = "new placeholder", copyTitle = { "Copy of $it" })

class EditorTestRepository private constructor(
    private val inner: FakeTemplateRepository,
    private val originals: List<Template>,
) : TemplateRepository by inner {
    constructor(initial: List<Template>, originals: List<Template> = emptyList()) : this(FakeTemplateRepository(initial), originals)

    val snapshot: List<Template> get() = inner.snapshot

    override suspend fun restoreBuiltIn(id: String) {
        val original = originals.firstOrNull { it.id == id } ?: return
        if (snapshot.any { it.id == id }) inner.update(original) else inner.add(original)
    }
}
