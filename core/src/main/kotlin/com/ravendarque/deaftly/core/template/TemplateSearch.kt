package com.ravendarque.deaftly.core.template

import java.text.Normalizer
import java.util.Locale

public object TemplateSearch {
    private val combiningMarks = Regex("\\p{Mn}+")

    public fun matches(
        template: Template,
        query: String,
    ): Boolean {
        val needle = fold(query)
        if (needle.isEmpty()) return true
        val text = TemplateRenderer.render(template, TemplateFill()).text
        return needle in fold(template.title) || needle in fold(text)
    }

    public fun fold(text: String): String =
        Normalizer
            .normalize(text.trim(), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
}
