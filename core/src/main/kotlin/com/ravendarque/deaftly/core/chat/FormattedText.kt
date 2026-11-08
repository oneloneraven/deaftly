package com.ravendarque.deaftly.core.chat

public data class FormattedText(
    val text: String = "",
    val ranges: List<StyleRange> = emptyList(),
) {
    init {
        require(ranges.all { it.end <= text.length }) { "A style range must lie within the text" }
    }

    public val spans: List<Span> get() = RichText.spans(text, ranges)

    public val isBlank: Boolean get() = text.isBlank()

    public fun replace(
        start: Int,
        end: Int,
        insertion: String,
        insertionStyles: Set<SpanStyle> = emptySet(),
    ): FormattedText {
        require(start in 0..end && end <= text.length) { "The replaced range must lie within the text" }
        val shift = insertion.length - (end - start)
        val kept =
            ranges.flatMap { range ->
                listOfNotNull(
                    clip(range, 0, start),
                    clip(range, end, text.length)?.let { StyleRange(it.start + shift, it.end + shift, it.style) },
                )
            }
        val inserted = if (insertion.isEmpty()) emptyList() else insertionStyles.map { StyleRange(start, start + insertion.length, it) }
        return FormattedText(text.substring(0, start) + insertion + text.substring(end), merge(kept + inserted))
    }

    public fun stylesBefore(index: Int): Set<SpanStyle> = stylesOf(index - 1)

    public fun isStyled(
        style: SpanStyle,
        start: Int,
        end: Int,
    ): Boolean = start < end && ranges.any { it.style == style && it.start <= start && end <= it.end }

    public fun toggle(
        style: SpanStyle,
        start: Int,
        end: Int,
    ): FormattedText {
        require(start in 0..end && end <= text.length) { "The range must lie within the text" }
        if (start == end) return this
        val others = ranges.filter { it.style != style }
        val same = ranges.filter { it.style == style }
        val updated =
            if (isStyled(style, start, end)) {
                same.flatMap { listOfNotNull(clip(it, 0, start), clip(it, end, text.length)) }
            } else {
                same + StyleRange(start, end, style)
            }
        return copy(ranges = merge(others + updated))
    }

    public fun trimmed(): FormattedText {
        val leading = text.length - text.trimStart().length
        val withoutLeading = replace(0, leading, "")
        return withoutLeading.replace(withoutLeading.text.trimEnd().length, withoutLeading.text.length, "")
    }

    private fun stylesOf(index: Int): Set<SpanStyle> =
        if (index !in text.indices) emptySet() else ranges.filter { it.start <= index && index < it.end }.mapTo(mutableSetOf()) { it.style }

    private fun clip(
        range: StyleRange,
        from: Int,
        to: Int,
    ): StyleRange? {
        val start = maxOf(range.start, from)
        val end = minOf(range.end, to)
        return if (start < end) StyleRange(start, end, range.style) else null
    }

    private fun merge(candidates: List<StyleRange>): List<StyleRange> =
        SpanStyle.entries.flatMap { style ->
            val merged = ArrayList<StyleRange>()
            for (range in candidates.filter { it.style == style && it.start < it.end }.sortedBy { it.start }) {
                val last = merged.lastOrNull()
                if (last != null && range.start <= last.end) {
                    merged[merged.lastIndex] = last.copy(end = maxOf(last.end, range.end))
                } else {
                    merged += range
                }
            }
            merged
        }
}
