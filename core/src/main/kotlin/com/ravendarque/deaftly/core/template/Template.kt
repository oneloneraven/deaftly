package com.ravendarque.deaftly.core.template

public enum class PlaceholderType { Choices, Typing, Camera }

public data class Placeholder(
    val key: String,
    val label: String,
    val type: PlaceholderType,
    val options: List<String> = emptyList(),
    val repeatable: Boolean = false,
)

public data class Template(
    val id: String,
    val title: String,
    val category: String,
    val body: String,
    val pinned: Boolean = false,
    val builtIn: Boolean = false,
    val placeholders: Map<String, Placeholder> = emptyMap(),
) {
    init {
        placeholders.forEach { (key, placeholder) ->
            require(key == placeholder.key) { "Placeholder map key must match the placeholder key" }
        }
    }

    public fun placeholder(key: String): Placeholder =
        placeholders[key] ?: Placeholder(key = key, label = key, type = PlaceholderType.Typing)

    public fun segments(): List<TemplateSegment> = TemplateParser.parse(body)
}
