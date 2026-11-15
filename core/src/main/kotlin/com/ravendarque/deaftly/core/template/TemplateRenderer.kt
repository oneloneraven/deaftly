package com.ravendarque.deaftly.core.template

public sealed interface RenderedPart {
    public data class Text(
        val text: String,
    ) : RenderedPart

    public data class Value(
        val key: String,
        val text: String,
        val valueSpans: List<TextSpan> = listOf(TextSpan(0, text.length)),
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
                        is RenderedPart.Text -> {
                            part.text.length
                        }

                        is RenderedPart.Value -> {
                            part.text.length.also {
                                part.valueSpans.forEach {
                                    add(
                                        TextSpan(
                                            offset + it.start,
                                            offset + it.end,
                                        ),
                                    )
                                }
                            }
                        }

                        is RenderedPart.Unfilled -> {
                            part.label.length
                        }
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
                    val values = values(placeholder, fill)
                    if (values.isEmpty()) {
                        unfilled += segment.key
                        parts += RenderedPart.Unfilled(segment.key, placeholder.label)
                    } else {
                        parts += joined(segment.key, values, placeholder.join)
                    }
                }
            }
        }
        return Rendered(parts, unfilled.toList())
    }

    public fun join(
        values: List<String>,
        join: Join = Join.And,
    ): String = joined("", cleaned(values), join).text

    private fun joined(
        key: String,
        values: List<String>,
        join: Join,
    ): RenderedPart.Value {
        val text = StringBuilder()
        val spans =
            values.mapIndexed { index, value ->
                when {
                    index == 0 -> Unit
                    index == values.lastIndex -> text.append(" ${join.word()} ")
                    else -> text.append(", ")
                }
                TextSpan(text.length, text.length + value.length).also { text.append(value) }
            }
        return RenderedPart.Value(key, text.toString(), spans)
    }

    private fun cleaned(values: List<String>) = values.map { it.trim() }.filter { it.isNotEmpty() }

    private fun Join.word() =
        when (this) {
            Join.And -> "and"
            Join.Or -> "or"
        }

    private fun values(
        placeholder: Placeholder,
        fill: TemplateFill,
    ): List<String> = cleaned(fill.values(placeholder.key)).let { if (placeholder.repeatable) it else it.take(1) }
}
