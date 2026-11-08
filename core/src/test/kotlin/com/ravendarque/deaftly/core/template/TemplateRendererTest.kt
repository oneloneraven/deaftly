package com.ravendarque.deaftly.core.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateRendererTest {
    private fun template(
        body: String,
        vararg placeholders: Placeholder,
    ) = Template(
        id = "t",
        title = "T",
        category = "c",
        body = body,
        placeholders = placeholders.associateBy { it.key },
    )

    private fun single(
        key: String,
        label: String = key,
    ) = Placeholder(key, label, PlaceholderType.Choices)

    private fun repeatable(
        key: String,
        label: String = key,
    ) = Placeholder(key, label, PlaceholderType.Choices, repeatable = true)

    @Test
    fun joinsFollowBritishEnglishWithoutOxfordComma() {
        assertEquals("", TemplateRenderer.join(emptyList()))
        assertEquals("A", TemplateRenderer.join(listOf("A")))
        assertEquals("A and B", TemplateRenderer.join(listOf("A", "B")))
        assertEquals("A, B and C", TemplateRenderer.join(listOf("A", "B", "C")))
        assertEquals("A, B, C and D", TemplateRenderer.join(listOf("A", "B", "C", "D")))
    }

    @Test
    fun joinTrimsAndIgnoresEmptyValues() {
        assertEquals("A and B", TemplateRenderer.join(listOf("  A ", "", "   ", "B ")))
        assertEquals("", TemplateRenderer.join(listOf("", "  ")))
    }

    @Test
    fun rendersAFilledSingleValue() {
        val template = template("My name is {name}.", single("name"))
        val rendered = TemplateRenderer.render(template, TemplateFill().set("name", "Sam"))
        assertEquals("My name is Sam.", rendered.text)
        assertTrue(rendered.isComplete)
        assertEquals(emptyList(), rendered.unfilledKeys)
    }

    @Test
    fun rendersRepeatableValuesJoined() {
        val template = template("Could I have {item} please?", repeatable("item"))
        val fill = TemplateFill().add("item", "tea").add("item", "a scone").add("item", "jam")
        assertEquals("Could I have tea, a scone and jam please?", TemplateRenderer.render(template, fill).text)
    }

    @Test
    fun nonRepeatableUsesTheFirstNonEmptyValueOnly() {
        val template = template("{a}", single("a"))
        val fill = TemplateFill().add("a", "  ").add("a", "first").add("a", "second")
        assertEquals("first", TemplateRenderer.render(template, fill).text)
    }

    @Test
    fun nonRepeatableValueIsTrimmed() {
        val template = template("[{a}]", single("a"))
        assertEquals("[x]", TemplateRenderer.render(template, TemplateFill().set("a", "  x \n")).text)
    }

    @Test
    fun unfilledPlaceholderShowsItsLabelAndIsReported() {
        val template =
            template(
                "Hi {name}, {how to reply}",
                single("name"),
                single("how to reply", "How should they reply?"),
            )
        val rendered = TemplateRenderer.render(template, TemplateFill().set("name", "Sam"))
        assertEquals("Hi Sam, How should they reply?", rendered.text)
        assertEquals(listOf("how to reply"), rendered.unfilledKeys)
        assertFalse(rendered.isComplete)
    }

    @Test
    fun emptyAndWhitespaceValuesCountAsUnfilled() {
        val template = template("{a} {b} {c}", single("a"), single("b"), repeatable("c"))
        val fill =
            TemplateFill()
                .set("a", "")
                .set("b", "   ")
                .add("c", "")
                .add("c", " ")
        val rendered = TemplateRenderer.render(template, fill)
        assertEquals(listOf("a", "b", "c"), rendered.unfilledKeys)
        assertFalse(rendered.isComplete)
    }

    @Test
    fun unfilledKeysAreInOrderOfFirstAppearanceWithoutDuplicates() {
        val template = template("{c} {a} {c} {b} {a}", single("a"), single("b"), single("c"))
        val rendered = TemplateRenderer.render(template, TemplateFill())
        assertEquals(listOf("c", "a", "b"), rendered.unfilledKeys)
    }

    @Test
    fun repeatedKeyRendersTheSameValueEverywhere() {
        val template = template("{n}, hello {n}!", single("n"))
        val rendered = TemplateRenderer.render(template, TemplateFill().set("n", "Zoë"))
        assertEquals("Zoë, hello Zoë!", rendered.text)
        assertEquals(
            listOf(
                RenderedPart.Value("n", "Zoë"),
                RenderedPart.Text(", hello "),
                RenderedPart.Value("n", "Zoë"),
                RenderedPart.Text("!"),
            ),
            rendered.parts,
        )
    }

    @Test
    fun unknownKeyIsATypingPlaceholderLabelledByItsKey() {
        val template = template("Hello {mystery}")
        val unfilled = TemplateRenderer.render(template, TemplateFill())
        assertEquals("Hello mystery", unfilled.text)
        assertEquals(listOf(RenderedPart.Text("Hello "), RenderedPart.Unfilled("mystery", "mystery")), unfilled.parts)
        assertEquals(listOf("mystery"), unfilled.unfilledKeys)

        val filled = TemplateRenderer.render(template, TemplateFill().set("mystery", "world"))
        assertEquals("Hello world", filled.text)
        assertTrue(filled.isComplete)
    }

    @Test
    fun unknownKeyIsSingleValuedEvenIfSeveralValuesAreHeld() {
        val template = template("{x}")
        val fill = TemplateFill().add("x", "a").add("x", "b")
        assertEquals("a", TemplateRenderer.render(template, fill).text)
    }

    @Test
    fun partsExposeTextValueAndUnfilledForHighlighting() {
        val template =
            template(
                "My name is {name}. {how to reply}",
                single("name"),
                single("how to reply", "Reply how?"),
            )
        val rendered = TemplateRenderer.render(template, TemplateFill().set("name", "Sam"))
        assertEquals(
            listOf(
                RenderedPart.Text("My name is "),
                RenderedPart.Value("name", "Sam"),
                RenderedPart.Text(". "),
                RenderedPart.Unfilled("how to reply", "Reply how?"),
            ),
            rendered.parts,
        )
    }

    @Test
    fun escapedBracesAppearAsLiteralText() {
        val template = template("Use {{braces}} for {thing}", single("thing"))
        assertEquals("Use {braces} for tea", TemplateRenderer.render(template, TemplateFill().set("thing", "tea")).text)
    }

    @Test
    fun malformedTokensRenderAsTheyWereTyped() {
        val template = template("a {} b {unclosed")
        val rendered = TemplateRenderer.render(template, TemplateFill())
        assertEquals("a {} b {unclosed", rendered.text)
        assertTrue(rendered.isComplete)
    }

    @Test
    fun unicodeAndAccentsSurviveRendering() {
        val template = template("Pourrais-je avoir {plat} ? ☕", repeatable("plat"))
        val fill = TemplateFill().add("plat", "une crème brûlée").add("plat", "un café 😀")
        assertEquals(
            "Pourrais-je avoir une crème brûlée and un café 😀 ? ☕",
            TemplateRenderer.render(template, fill).text,
        )
    }

    @Test
    fun templateWithNoPlaceholdersIsCompleteAndUnchanged() {
        val rendered = TemplateRenderer.render(template("Hello there"), TemplateFill())
        assertEquals("Hello there", rendered.text)
        assertTrue(rendered.isComplete)
        assertEquals(listOf(RenderedPart.Text("Hello there")), rendered.parts)
    }

    @Test
    fun emptyTemplateRendersEmpty() {
        val rendered = TemplateRenderer.render(template(""), TemplateFill())
        assertEquals("", rendered.text)
        assertEquals(emptyList(), rendered.parts)
        assertTrue(rendered.isComplete)
    }

    @Test
    fun valuesContainingBracesAreNotReparsed() {
        val template = template("{a}", single("a"))
        assertEquals("{b}", TemplateRenderer.render(template, TemplateFill().set("a", "{b}")).text)
    }

    @Test
    fun valuesForKeysNotInTheBodyAreIgnored() {
        val template = template("Hi", single("name"))
        val rendered = TemplateRenderer.render(template, TemplateFill().set("name", "Sam"))
        assertEquals("Hi", rendered.text)
        assertTrue(rendered.isComplete)
    }

    @Test
    fun valueSpansCoverEachFilledValueInTheRenderedText() {
        val template = template("Hi {a}, I am {b}.", single("a"), single("b"))
        val rendered = TemplateRenderer.render(template, TemplateFill().set("a", "Pat").set("b", " Sam "))
        assertEquals("Hi Pat, I am Sam.", rendered.text)
        assertEquals(listOf(TextSpan(3, 6), TextSpan(13, 16)), rendered.valueSpans)
        assertEquals(listOf("Pat", "Sam"), rendered.valueSpans.map { rendered.text.substring(it.start, it.end) })
    }

    @Test
    fun aRepeatableValueIsOneSpanOverTheWholeJoinedText() {
        val template = template("Could I have {item} please?", repeatable("item"))
        val fill = TemplateFill().add("item", "tea").add("item", "a scone").add("item", "jam")
        val rendered = TemplateRenderer.render(template, fill)
        assertEquals(listOf("tea, a scone and jam"), rendered.valueSpans.map { rendered.text.substring(it.start, it.end) })
    }

    @Test
    fun spansDoNotDependOnTheValueAppearingEarlierInTheText() {
        val template = template("Sam says {x}. Sam is {y}.", single("x"), single("y"))
        val rendered = TemplateRenderer.render(template, TemplateFill().set("x", "Sam").set("y", "Sam"))
        assertEquals(listOf(TextSpan(9, 12), TextSpan(21, 24)), rendered.valueSpans)
    }

    @Test
    fun unfilledPlaceholdersAndPlainTextHaveNoSpans() {
        val template = template("Hello {name}.", single("name"))
        assertEquals(emptyList(), TemplateRenderer.render(template, TemplateFill()).valueSpans)
        assertEquals(emptyList(), TemplateRenderer.render(template("Plain."), TemplateFill()).valueSpans)
    }
}
