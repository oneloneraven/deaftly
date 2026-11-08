package com.ravendarque.deaftly.core.template

public object TemplateParser {
    private const val OPEN = '{'
    private const val CLOSE = '}'

    public fun parse(source: String): List<TemplateSegment> {
        val segments = mutableListOf<TemplateSegment>()
        val text = StringBuilder()
        var index = 0
        while (index < source.length) {
            val char = source[index]
            if (isEscape(source, index)) {
                text.append(char)
                index += 2
            } else if (char == OPEN) {
                index = parseToken(source, index, segments, text)
            } else {
                text.append(char)
                index++
            }
        }
        flush(segments, text)
        return segments
    }

    private fun isEscape(
        source: String,
        index: Int,
    ): Boolean {
        val char = source[index]
        return (char == OPEN || char == CLOSE) && source.getOrNull(index + 1) == char
    }

    private fun parseToken(
        source: String,
        openIndex: Int,
        segments: MutableList<TemplateSegment>,
        text: StringBuilder,
    ): Int {
        val end = closingIndex(source, openIndex)
        if (end < 0) {
            text.append(OPEN)
            return openIndex + 1
        }
        val inner = source.substring(openIndex + 1, end)
        if (OPEN in inner || inner.isBlank()) {
            text.append(source, openIndex, end + 1)
        } else {
            flush(segments, text)
            segments += TemplateSegment.PlaceholderRef(inner)
        }
        return end + 1
    }

    private fun flush(
        segments: MutableList<TemplateSegment>,
        text: StringBuilder,
    ) {
        if (text.isNotEmpty()) {
            segments += TemplateSegment.Text(text.toString())
            text.setLength(0)
        }
    }

    private fun closingIndex(
        source: String,
        openIndex: Int,
    ): Int {
        var depth = 0
        for (i in openIndex until source.length) {
            when (source[i]) {
                OPEN -> depth++
                CLOSE -> if (--depth == 0) return i
            }
        }
        return -1
    }
}
