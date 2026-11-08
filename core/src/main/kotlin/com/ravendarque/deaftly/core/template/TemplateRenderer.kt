package com.ravendarque.deaftly.core.template

public sealed interface RenderedPart {
    public data class Text(
        val text: String,
    ) : RenderedPart

    public data class Value(
        val key: String,
        val text: String,
    ) : RenderedPart

    public data class Unfilled(
        val key: String,
        val label: String,
    ) : RenderedPart
}

public data class TextSpan(
    val start: Int,
    val end: Int,
)

public data class Rendered(
    val parts: List<RenderedPart>,
    val unfilledKeys: List<String>,
) {
    val text: String =
        parts.joinToString("") {
            when (it) {
                is RenderedPart.Text -> it.text
                is RenderedPart.Value -> it.text
                is RenderedPart.Unfilled -> it.label
            }
        }

    val valueSpans: List<TextSpan> =
        buildList {
            var offset = 0
            for (part in parts) {
                val length =
                    when (part) {
                        is RenderedPart.Text -> part.text.length
                        is RenderedPart.Value -> part.text.length.also { add(TextSpan(offset, offset + it)) }
                        is RenderedPart.Unfilled -> part.label.length
                    }
                offset += length
            }
        }

    val isComplete: Boolean get() = unfilledKeys.isEmpty()
}

public object TemplateRenderer {
    public fun render(
        template: Template,
        fill: TemplateFill,
    ): Rendered {
        val parts = mutableListOf<RenderedPart>()
        val unfilled = linkedSetOf<String>()
        for (segment in template.segments()) {
            when (segment) {
                is TemplateSegment.Text -> {
                    parts += RenderedPart.Text(segment.value)
                }

                is TemplateSegment.PlaceholderRef -> {
                    val placeholder = template.placeholder(segment.key)
                    val text = valueText(placeholder, fill)
                    if (text.isEmpty()) {
                        unfilled += segment.key
                        parts += RenderedPart.Unfilled(segment.key, placeholder.label)
                    } else {
                        parts += RenderedPart.Value(segment.key, text)
                    }
                }
            }
        }
        return Rendered(parts, unfilled.toList())
    }

    public fun join(values: List<String>): String {
        val cleaned = values.map { it.trim() }.filter { it.isNotEmpty() }
        return when (cleaned.size) {
            0 -> ""
            1 -> cleaned.first()
            else -> cleaned.dropLast(1).joinToString(", ") + " and " + cleaned.last()
        }
    }

    private fun valueText(
        placeholder: Placeholder,
        fill: TemplateFill,
    ): String =
        if (placeholder.repeatable) {
            join(fill.values(placeholder.key))
        } else {
            fill
                .values(placeholder.key)
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()
        }
}
