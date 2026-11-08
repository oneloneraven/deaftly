package com.ravendarque.deaftly.core.chat

public data class Composition(
    val content: FormattedText = FormattedText(),
    val selectionStart: Int = content.text.length,
    val selectionEnd: Int = selectionStart,
    val typing: Set<SpanStyle>? = null,
) {
    init {
        require(selectionStart in 0..selectionEnd && selectionEnd <= content.text.length) {
            "The selection must lie within the text"
        }
    }

    public val text: String get() = content.text

    public val hasSelection: Boolean get() = selectionStart != selectionEnd

    public val isBlank: Boolean get() = content.isBlank

    public fun isActive(style: SpanStyle): Boolean =
        if (hasSelection) content.isStyled(style, selectionStart, selectionEnd) else style in typingStyles(selectionStart)

    public fun toggle(style: SpanStyle): Composition =
        if (hasSelection) {
            copy(content = content.toggle(style, selectionStart, selectionEnd))
        } else {
            val current = typingStyles(selectionStart)
            copy(typing = if (style in current) current - style else current + style)
        }

    public fun select(
        start: Int,
        end: Int,
    ): Composition {
        val from = start.coerceIn(0, text.length)
        val to = end.coerceIn(0, text.length)
        val low = minOf(from, to)
        val high = maxOf(from, to)
        return if (low == selectionStart && high == selectionEnd) this else copy(selectionStart = low, selectionEnd = high, typing = null)
    }

    public fun insert(insertion: String): Composition = edit(selectionStart, selectionEnd, insertion)

    public fun withText(
        newText: String,
        start: Int,
        end: Int,
    ): Composition {
        if (newText == text) return select(start, end)
        val prefix = commonPrefix(text, newText)
        val suffix = commonSuffix(text, newText, prefix)
        return edit(prefix, text.length - suffix, newText.substring(prefix, newText.length - suffix), start, end)
    }

    private fun edit(
        from: Int,
        to: Int,
        insertion: String,
        start: Int = from + insertion.length,
        end: Int = start,
    ): Composition {
        val updated = content.replace(from, to, insertion, typingStyles(from))
        val low = minOf(start, end).coerceIn(0, updated.text.length)
        val high = maxOf(start, end).coerceIn(0, updated.text.length)
        return Composition(updated, low, high)
    }

    private fun typingStyles(at: Int): Set<SpanStyle> = typing ?: content.stylesBefore(at)

    private fun commonPrefix(
        old: String,
        new: String,
    ): Int {
        var length = 0
        val limit = minOf(old.length, new.length)
        while (length < limit && old[length] == new[length]) length++
        if (length in 1 until limit && old[length - 1].isHighSurrogate()) length--
        return length
    }

    private fun commonSuffix(
        old: String,
        new: String,
        prefix: Int,
    ): Int {
        var length = 0
        val limit = minOf(old.length, new.length) - prefix
        while (length < limit && old[old.length - 1 - length] == new[new.length - 1 - length]) length++
        if (length > 0 && length < old.length && old[old.length - length].isLowSurrogate()) length--
        return length
    }
}
