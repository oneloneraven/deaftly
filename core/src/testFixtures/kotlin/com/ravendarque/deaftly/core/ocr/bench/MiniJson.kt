package com.ravendarque.deaftly.core.ocr.bench

public object MiniJson {
    public fun parse(source: String): Any? = Reader(source).parse()

    public fun write(value: Any?): String = StringBuilder().also { append(it, value) }.toString()

    private fun append(
        out: StringBuilder,
        value: Any?,
    ) {
        when (value) {
            null -> {
                out.append("null")
            }

            is String -> {
                appendString(out, value)
            }

            is Float, is Double -> {
                out.append(if ((value as Number).toDouble().isFinite()) value.toString() else "null")
            }

            is Number, is Boolean -> {
                out.append(value.toString())
            }

            is Map<*, *> -> {
                out.append('{')
                value.entries.forEachIndexed { index, (key, item) ->
                    if (index > 0) out.append(',')
                    appendString(out, key.toString())
                    out.append(':')
                    append(out, item)
                }
                out.append('}')
            }

            is Iterable<*> -> {
                out.append('[')
                value.forEachIndexed { index, item ->
                    if (index > 0) out.append(',')
                    append(out, item)
                }
                out.append(']')
            }

            else -> {
                error("Unsupported JSON value ${value::class}")
            }
        }
    }

    private fun appendString(
        out: StringBuilder,
        text: String,
    ) {
        out.append('"')
        for (c in text) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c < ' ' -> out.append("\\u%04x".format(c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Reader(
        private val source: String,
    ) {
        private var index = 0

        fun parse(): Any? =
            value().also {
                skipSpace()
                check(index == source.length) { "Trailing content at $index" }
            }

        private fun skipSpace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        private fun value(): Any? {
            skipSpace()
            return when (val c = source[index]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> string()
                else -> literal(c)
            }
        }

        private fun obj(): Map<String, Any?> {
            val map = linkedMapOf<String, Any?>()
            index++
            skipSpace()
            if (source[index] == '}') return map.also { index++ }
            do {
                skipSpace()
                val key = string()
                skipSpace()
                check(source[index++] == ':') { "Expected colon at $index" }
                map[key] = value()
                skipSpace()
            } while (source[index++] == ',')
            check(source[index - 1] == '}') { "Expected closing brace at $index" }
            return map
        }

        private fun arr(): List<Any?> {
            val list = mutableListOf<Any?>()
            index++
            skipSpace()
            if (source[index] == ']') return list.also { index++ }
            do {
                list.add(value())
                skipSpace()
            } while (source[index++] == ',')
            check(source[index - 1] == ']') { "Expected closing bracket at $index" }
            return list
        }

        private fun string(): String {
            skipSpace()
            check(source[index++] == '"') { "Expected string at $index" }
            val out = StringBuilder()
            while (source[index] != '"') {
                val c = source[index++]
                if (c != '\\') {
                    out.append(c)
                    continue
                }
                when (val e = source[index++]) {
                    'n' -> {
                        out.append('\n')
                    }

                    'u' -> {
                        out.append(source.substring(index, index + HEX_DIGITS).toInt(HEX_RADIX).toChar())
                        index += HEX_DIGITS
                    }

                    else -> {
                        out.append(e)
                    }
                }
            }
            index++
            return out.toString()
        }

        private fun literal(first: Char): Any? {
            val start = index
            while (index < source.length && source[index] !in ",]} \n\r\t") index++
            val token = source.substring(start, index)
            return when {
                token == "true" -> true
                token == "false" -> false
                token == "null" -> null
                first == '-' || first.isDigit() -> token.toDouble()
                else -> error("Unexpected token '$token' at $start")
            }
        }
    }

    private const val HEX_DIGITS = 4
    private const val HEX_RADIX = 16
}
