package com.ravendarque.deaftly.help

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HelpTopicTest {
    @Test
    fun readsTheTitleOrderAndBody() {
        val topic = parseHelpTopic("templates", "---\ntitle: Templates\norder: 3\n---\n\n# Templates\n\nHello\n")

        assertEquals(HelpTopic("templates", "Templates", 3, "# Templates\n\nHello"), topic?.copy(body = topic.body.trimEnd()))
    }

    @Test
    fun toleratesWindowsLineEndings() {
        val topic = parseHelpTopic("a", "---\r\ntitle: A\r\norder: 2\r\n---\r\n\r\n# A\r\n")

        assertEquals("A", topic?.title)
        assertEquals(2, topic?.order)
        assertEquals("# A", topic?.body?.trim())
    }

    @Test
    fun stripsQuotesAroundTheTitle() {
        assertEquals("Show and Type & show", parseHelpTopic("a", "---\ntitle: \"Show and Type & show\"\norder: 1\n---\nx")?.title)
        assertEquals("It's here", parseHelpTopic("a", "---\ntitle: 'It's here'\norder: 1\n---\nx")?.title)
    }

    @Test
    fun keepsAColonInsideTheTitle() {
        assertEquals("Tip: be brief", parseHelpTopic("a", "---\ntitle: Tip: be brief\norder: 1\n---\nx")?.title)
    }

    @Test
    fun leavesNoFrontMatterInTheBody() {
        val body = parseHelpTopic("a", "---\ntitle: A\norder: 1\n---\n# A\n")?.body.orEmpty()

        assertEquals(false, body.contains("order:"))
        assertEquals(false, body.contains("---"))
    }

    @Test
    fun rejectsPagesWithoutUsableFrontMatter() {
        assertNull(parseHelpTopic("a", "# No front matter"))
        assertNull(parseHelpTopic("a", "---\ntitle: A\n---\nx"))
        assertNull(parseHelpTopic("a", "---\norder: 1\n---\nx"))
        assertNull(parseHelpTopic("a", "---\ntitle: A\norder: first\n---\nx"))
        assertNull(parseHelpTopic("a", "---\ntitle: A\norder: 1\nx"))
    }
}
