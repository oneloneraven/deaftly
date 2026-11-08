package com.ravendarque.deaftly.core.template

public object TemplateSerialiser {
    public fun serialise(segments: List<TemplateSegment>): String =
        segments.joinToString("") { segment ->
            when (segment) {
                is TemplateSegment.Text -> segment.value.replace("{", "{{").replace("}", "}}")
                is TemplateSegment.PlaceholderRef -> "{${segment.key}}"
            }
        }
}
