package com.ravendarque.deaftly.core.template

public sealed interface TemplateSegment {
    public data class Text(
        val value: String,
    ) : TemplateSegment

    public data class PlaceholderRef(
        val key: String,
    ) : TemplateSegment {
        init {
            require(key.isNotBlank()) { "Placeholder key must not be blank" }
            require('{' !in key && '}' !in key) { "Placeholder key must not contain braces" }
        }
    }
}
