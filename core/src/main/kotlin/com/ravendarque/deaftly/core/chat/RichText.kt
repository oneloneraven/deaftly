package com.ravendarque.deaftly.core.chat

public enum class SpanStyle { Bold, Italic, Highlight }

public data class Span(
    val text: String,
    val styles: Set<SpanStyle> = emptySet(),
) {
    init {
        require(text.isNotEmpty()) { "A span must not be empty" }
    }
}

public data class StyleRange(
    val start: Int,
    val end: Int,
    val style: SpanStyle,
) {
    init {
        require(start in 0..end) { "A style range must satisfy 0 <= start <= end" }
    }
}

public object RichText {
    public fun spans(
        text: String,
        ranges: List<StyleRange> = emptyList(),
    ): List<Span> {
        ranges.forEach { range ->
            require(range.end <= text.length) { "A style range must lie within the text" }
            require(isCharacterBoundary(text, range.start) && isCharacterBoundary(text, range.end)) {
                "A style range must not split a character"
            }
        }
        if (text.isEmpty()) return emptyList()
        val cuts = (ranges.flatMap { listOf(it.start, it.end) } + 0 + text.length).toSortedSet().toList()
        val runs = ArrayList<Span>()
        for ((from, to) in cuts.zipWithNext()) {
            val styles = ranges.filter { it.start <= from && to <= it.end }.mapTo(mutableSetOf()) { it.style }
            val last = runs.lastOrNull()
            if (last != null && last.styles == styles) {
                runs[runs.lastIndex] = last.copy(text = last.text + text.substring(from, to))
            } else {
                runs += Span(text.substring(from, to), styles)
            }
        }
        return runs
    }

    public fun plainText(spans: List<Span>): String = spans.joinToString("") { it.text }

    private fun isCharacterBoundary(
        text: String,
        index: Int,
    ): Boolean = index <= 0 || index >= text.length || !(text[index - 1].isHighSurrogate() && text[index].isLowSurrogate())
}
