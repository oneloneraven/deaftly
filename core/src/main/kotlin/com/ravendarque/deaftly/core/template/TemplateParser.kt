package com.ravendarque.deaftly.core.template

public data class PlaceholderToken(
    val key: String,
    val start: Int,
    val end: Int,
)

public object TemplateParser {
    private const val OPEN = '{'
    private const val CLOSE = '}'

    public fun parse(source: String): List<TemplateSegment> = Scan(source).run().segments

    public fun tokens(source: String): List<PlaceholderToken> = Scan(source).run().tokens

    private class Scan(
        private val source: String,
    ) {
        val segments = mutableListOf<TemplateSegment>()
        val tokens = mutableListOf<PlaceholderToken>()
        private val text = StringBuilder()

        fun run(): Scan {
            var index = 0
            while (index < source.length) {
                val char = source[index]
                if (isEscape(index)) {
                    text.append(char)
                    index += 2
                } else if (char == OPEN) {
                    index = parseToken(index)
                } else {
                    text.append(char)
                    index++
                }
            }
            flush()
            return this
        }

        private fun isEscape(index: Int): Boolean {
            val char = source[index]
            return (char == OPEN || char == CLOSE) && source.getOrNull(index + 1) == char
        }

        private fun parseToken(openIndex: Int): Int {
            val end = closingIndex(openIndex)
            if (end < 0) {
                text.append(OPEN)
                return openIndex + 1
            }
            val inner = source.substring(openIndex + 1, end)
            if (OPEN in inner || inner.isBlank()) {
                text.append(source, openIndex, end + 1)
            } else {
                flush()
                segments += TemplateSegment.PlaceholderRef(inner)
                tokens += PlaceholderToken(inner, openIndex, end + 1)
            }
            return end + 1
        }

        private fun flush() {
            if (text.isNotEmpty()) {
                segments += TemplateSegment.Text(text.toString())
                text.setLength(0)
            }
        }

        private fun closingIndex(openIndex: Int): Int {
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
}
