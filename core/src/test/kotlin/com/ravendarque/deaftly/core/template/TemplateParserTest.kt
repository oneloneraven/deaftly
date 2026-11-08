package com.ravendarque.deaftly.core.template

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TemplateParserTest {
    private fun text(value: String) = TemplateSegment.Text(value)

    private fun ref(key: String) = TemplateSegment.PlaceholderRef(key)

    @Test
    fun parsesTextAndPlaceholders() {
        assertEquals(
            listOf(
                text("Hi! I'm deaf. Could I have "),
                ref("menu item"),
                text(" please? "),
                ref("how to reply"),
            ),
            TemplateParser.parse("Hi! I'm deaf. Could I have {menu item} please? {how to reply}"),
        )
    }

    @Test
    fun emptyInputGivesNoSegments() {
        assertEquals(emptyList(), TemplateParser.parse(""))
    }

    @Test
    fun plainTextIsOneSegment() {
        assertEquals(listOf(text("Hello there")), TemplateParser.parse("Hello there"))
    }

    @Test
    fun adjacentPlaceholdersStaySeparate() {
        assertEquals(listOf(ref("a"), ref("b")), TemplateParser.parse("{a}{b}"))
    }

    @Test
    fun doubledBracesEscapeToLiteralBraces() {
        assertEquals(listOf(text("a {b} c")), TemplateParser.parse("a {{b}} c"))
    }

    @Test
    fun escapedBraceBesidePlaceholder() {
        assertEquals(listOf(text("{"), ref("a"), text("}")), TemplateParser.parse("{{{a}}}"))
    }

    @Test
    fun unclosedBraceStaysText() {
        assertEquals(listOf(text("Hello {name")), TemplateParser.parse("Hello {name"))
    }

    @Test
    fun unclosedBraceDoesNotSwallowLaterPlaceholder() {
        assertEquals(listOf(text("a { b "), ref("c")), TemplateParser.parse("a { b {c}"))
    }

    @Test
    fun emptyTokenStaysText() {
        assertEquals(listOf(text("Hello {} there")), TemplateParser.parse("Hello {} there"))
    }

    @Test
    fun blankTokenStaysText() {
        assertEquals(listOf(text("Hello {  } there")), TemplateParser.parse("Hello {  } there"))
    }

    @Test
    fun nestedTokenStaysText() {
        assertEquals(listOf(text("x {a{b}} y")), TemplateParser.parse("x {a{b}} y"))
    }

    @Test
    fun loneClosingBraceStaysText() {
        assertEquals(listOf(text("a } b")), TemplateParser.parse("a } b"))
    }

    @Test
    fun unicodeKeysAndTextSurvive() {
        assertEquals(
            listOf(text("Café ☕ "), ref("crème brûlée"), text(" 😀")),
            TemplateParser.parse("Café ☕ {crème brûlée} 😀"),
        )
    }

    @Test
    fun keysAreKeptVerbatim() {
        assertEquals(listOf(ref(" a ")), TemplateParser.parse("{ a }"))
    }

    @Test
    fun serialiseEscapesBracesInText() {
        assertEquals("a {{b}} {c}", TemplateSerialiser.serialise(listOf(text("a {b} "), ref("c"))))
    }

    @Test
    fun placeholderRefRejectsBadKeys() {
        assertFailsWith<IllegalArgumentException> { ref("") }
        assertFailsWith<IllegalArgumentException> { ref("   ") }
        assertFailsWith<IllegalArgumentException> { ref("a{b") }
        assertFailsWith<IllegalArgumentException> { ref("a}b") }
    }

    @Test
    fun parseNeverThrowsOnArbitraryInput() {
        val random = Random(SEED)
        repeat(ITERATIONS) {
            TemplateParser.parse(randomString(random, ALPHABET_WITH_BRACES, MAX_LENGTH))
        }
    }

    @Test
    fun parsedOutputIsAlwaysNormalised() {
        val random = Random(SEED)
        repeat(ITERATIONS) {
            val segments = TemplateParser.parse(randomString(random, ALPHABET_WITH_BRACES, MAX_LENGTH))
            segments.forEachIndexed { index, segment ->
                if (segment is TemplateSegment.Text) {
                    assertTrue(segment.value.isNotEmpty())
                    assertTrue(segments.getOrNull(index - 1) !is TemplateSegment.Text)
                }
            }
        }
    }

    @Test
    fun parseOfSerialiseRoundTripsOverRandomSegments() {
        val random = Random(SEED)
        repeat(ITERATIONS) {
            val segments = randomSegments(random)
            assertEquals(segments, TemplateParser.parse(TemplateSerialiser.serialise(segments)))
        }
    }

    @Test
    fun serialiseOfParseRoundTripsOverRandomWellFormedStrings() {
        val random = Random(SEED)
        repeat(ITERATIONS) {
            val input = randomWellFormed(random)
            assertEquals(input, TemplateSerialiser.serialise(TemplateParser.parse(input)))
        }
    }

    @Test
    fun templateSegmentsDelegateToParser() {
        val template = Template(id = "t", title = "T", category = "c", body = "Hi {a}")
        assertEquals(listOf(text("Hi "), ref("a")), template.segments())
    }

    private fun randomString(
        random: Random,
        alphabet: List<String>,
        maxLength: Int,
    ): String = (0 until random.nextInt(maxLength + 1)).joinToString("") { alphabet[random.nextInt(alphabet.size)] }

    private fun randomSegments(random: Random): List<TemplateSegment> {
        val result = mutableListOf<TemplateSegment>()
        repeat(random.nextInt(MAX_SEGMENTS + 1)) {
            val previousIsText = result.lastOrNull() is TemplateSegment.Text
            if (previousIsText || random.nextBoolean()) {
                result += ref(randomKey(random))
            } else {
                val value = randomString(random, ALPHABET_WITH_BRACES, MAX_LENGTH)
                if (value.isNotEmpty()) result += text(value)
            }
        }
        return result
    }

    private fun randomKey(random: Random): String {
        while (true) {
            val key = randomString(random, ALPHABET_NO_BRACES, KEY_LENGTH)
            if (key.isNotBlank()) return key
        }
    }

    private fun randomWellFormed(random: Random): String =
        (0 until random.nextInt(MAX_SEGMENTS + 1)).joinToString("") {
            when (random.nextInt(PIECE_KINDS)) {
                0 -> "{{"
                1 -> "}}"
                2 -> "{" + randomKey(random) + "}"
                else -> randomString(random, ALPHABET_NO_BRACES, MAX_LENGTH)
            }
        }

    private companion object {
        const val SEED = 27L
        const val ITERATIONS = 2000
        const val MAX_LENGTH = 12
        const val MAX_SEGMENTS = 8
        const val KEY_LENGTH = 6
        const val PIECE_KINDS = 4
        val ALPHABET_NO_BRACES = listOf("a", "b", " ", "é", "ß", "😀", "x y", ".")
        val ALPHABET_WITH_BRACES = ALPHABET_NO_BRACES + listOf("{", "}", "{{", "}}")
    }
}
