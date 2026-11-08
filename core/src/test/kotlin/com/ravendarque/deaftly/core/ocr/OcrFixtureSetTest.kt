package com.ravendarque.deaftly.core.ocr

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrFixtureSetTest {
    private val directory = File("../app/src/androidTest/assets/ocr-fixtures")
    private val images = directory.listFiles { file -> file.extension == "jpg" }.orEmpty().sortedBy { it.name }

    @Test
    fun hasAtLeastThirtyFixturesUnderTheSizeBudget() {
        assertTrue(images.size >= MIN_FIXTURES, "Found ${images.size} fixtures")
        val total = directory.listFiles().orEmpty().sumOf { it.length() }
        assertTrue(total < MAX_BYTES, "Fixture set is $total bytes")
    }

    @Test
    fun everyImageHasJsonAndEveryJsonHasAnImage() {
        val jsonFiles = directory.listFiles { file -> file.extension == "json" }.orEmpty()
        val jsonNames = jsonFiles.map { it.nameWithoutExtension }
        assertEquals(images.map { it.nameWithoutExtension }.toSet(), jsonNames.toSet())
    }

    @Test
    fun everyGroundTruthParsesAndMatchesItsImage() {
        for (image in images) {
            val id = image.nameWithoutExtension
            val json = Json(File(directory, "$id.json").readText()).parse() as Map<*, *>
            val bitmap = checkNotNull(ImageIO.read(image)) { "$id: unreadable image" }
            assertEquals("$id.jpg", json["image"], id)
            assertEquals(bitmap.width, (json["width"] as Double).toInt(), id)
            assertEquals(bitmap.height, (json["height"] as Double).toInt(), id)
            val lines = json["lines"] as List<*>
            assertTrue(lines.isNotEmpty(), "$id: no lines")
            for (line in lines) {
                checkRegion(id, line as Map<*, *>, bitmap.width, bitmap.height)
                val words = line["words"] as List<*>
                assertEquals(
                    (line["text"] as String).trim().replace(Regex(" +"), " "),
                    words.joinToString(" ") { (it as Map<*, *>)["text"] as String },
                    "$id: words rebuild line",
                )
                words.forEach { checkRegion(id, it as Map<*, *>, bitmap.width, bitmap.height) }
            }
        }
    }

    private fun checkRegion(
        id: String,
        region: Map<*, *>,
        width: Int,
        height: Int,
    ) {
        val text = region["text"] as String
        assertTrue(text.isNotBlank(), "$id: blank text")
        val polygon = (region["polygon"] as List<*>).map { point -> (point as List<*>).map { it as Double } }
        assertEquals(CORNERS, polygon.size, "$id: '$text' polygon corners")
        for ((x, y) in polygon) {
            assertTrue(x in 0.0..width.toDouble() && y in 0.0..height.toDouble(), "$id: '$text' outside image")
        }
        val box = (region["box"] as List<*>).map { it as Double }
        assertEquals(polygon.minOf { it[0] }, box[0], TOLERANCE, "$id: '$text' box left")
        assertEquals(polygon.minOf { it[1] }, box[1], TOLERANCE, "$id: '$text' box top")
        assertEquals(polygon.maxOf { it[0] }, box[2], TOLERANCE, "$id: '$text' box right")
        assertEquals(polygon.maxOf { it[1] }, box[3], TOLERANCE, "$id: '$text' box bottom")
    }

    private class Json(
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

    private companion object {
        const val MIN_FIXTURES = 30
        const val MAX_BYTES = 15L * 1024 * 1024
        const val CORNERS = 4
        const val TOLERANCE = 0.11
        const val HEX_DIGITS = 4
        const val HEX_RADIX = 16
    }
}
