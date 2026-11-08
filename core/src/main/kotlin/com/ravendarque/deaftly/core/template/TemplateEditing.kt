package com.ravendarque.deaftly.core.template

public data class TextEdit(
    val text: String,
    val cursor: Int,
)

public class KeyRename(
    public val text: String,
    private val matches: List<PlaceholderToken>,
    private val delta: Int,
) {
    public fun mapOffset(offset: Int): Int {
        val inside = matches.indexOfFirst { offset > it.start && offset < it.end }
        return if (inside >= 0) {
            matches[inside].end + delta * (inside + 1)
        } else {
            offset + delta * matches.count { it.end <= offset }
        }
    }
}

public object TemplateEditing {
    public fun keys(body: String): List<String> = TemplateParser.tokens(body).map { it.key }.distinct()

    public fun tokenAt(
        body: String,
        cursor: Int,
    ): PlaceholderToken? = TemplateParser.tokens(body).firstOrNull { cursor > it.start && cursor <= it.end }

    public fun insertToken(
        body: String,
        selectionStart: Int,
        selectionEnd: Int,
        key: String,
    ): TextEdit {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, body.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, body.length)
        val token = "{$key}"
        return TextEdit(body.substring(0, start) + token + body.substring(end), start + token.length)
    }

    public fun renameKey(
        body: String,
        from: String,
        to: String,
    ): KeyRename {
        val matches = TemplateParser.tokens(body).filter { it.key == from }
        val builder = StringBuilder()
        var copied = 0
        matches.forEach { match ->
            builder
                .append(body, copied, match.start)
                .append('{')
                .append(to)
                .append('}')
            copied = match.end
        }
        builder.append(body, copied, body.length)
        return KeyRename(builder.toString(), matches, to.length - from.length)
    }

    public fun removeKey(
        body: String,
        key: String,
    ): String {
        val builder = StringBuilder()
        var copied = 0
        TemplateParser.tokens(body).filter { it.key == key }.forEach { match ->
            builder.append(body, copied, match.start)
            copied = match.end
            if (builder.lastOrNull() == ' ' && body.getOrNull(copied) == ' ') copied++
        }
        builder.append(body, copied, body.length)
        return builder.toString()
    }

    public fun nextFreeKey(
        base: String,
        taken: Collection<String>,
    ): String {
        if (base !in taken) return base
        var number = 2
        while ("$base $number" in taken) number++
        return "$base $number"
    }

    public fun keepTokensWhole(
        old: String,
        new: String,
    ): TextEdit? {
        if (old == new) return null
        val prefix = old.commonPrefixWith(new).length
        val suffix = old.commonSuffixWith(new).length.coerceAtMost(minOf(old.length, new.length) - prefix)
        val start = prefix
        val end = old.length - suffix
        val inserted = new.substring(prefix, new.length - suffix)
        val tokens = TemplateParser.tokens(old)
        val insideToken = if (start == end) tokens.firstOrNull { start > it.start && start < it.end } else null
        val touched = tokens.filter { it.start < end && it.end > start }
        return when {
            insideToken != null -> {
                splice(old, insideToken.end, insideToken.end, inserted)
            }

            touched.any { it.start < start || it.end > end } -> {
                splice(old, minOf(start, touched.minOf { it.start }), maxOf(end, touched.maxOf { it.end }), inserted)
            }

            else -> {
                null
            }
        }
    }

    private fun splice(
        text: String,
        start: Int,
        end: Int,
        inserted: String,
    ) = TextEdit(text.substring(0, start) + inserted + text.substring(end), start + inserted.length)
}
