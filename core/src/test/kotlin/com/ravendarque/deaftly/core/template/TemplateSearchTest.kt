package com.ravendarque.deaftly.core.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateSearchTest {
    private val template =
        Template(
            id = "t",
            title = "Café order",
            category = "Food",
            body = "Could I have {item} please?",
            placeholders = mapOf("item" to Placeholder("item", "Menu item", PlaceholderType.Typing)),
        )

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(TemplateSearch.matches(template, ""))
        assertTrue(TemplateSearch.matches(template, "   "))
    }

    @Test
    fun matchesTitleIgnoringCaseAndAccents() {
        assertTrue(TemplateSearch.matches(template, "CAFE"))
        assertTrue(TemplateSearch.matches(template, "café"))
        assertTrue(TemplateSearch.matches(template, "cafe order"))
    }

    @Test
    fun matchesAccentedQueryAgainstPlainText() {
        val plain = template.copy(title = "Cafe")
        assertTrue(TemplateSearch.matches(plain, "Café"))
    }

    @Test
    fun matchesBodyTextAndPlaceholderLabelsButNotPlaceholderKeys() {
        assertTrue(TemplateSearch.matches(template, "have"))
        assertTrue(TemplateSearch.matches(template, "menu item"))
        assertFalse(TemplateSearch.matches(template, "{item}"))
    }

    @Test
    fun doesNotMatchUnrelatedText() {
        assertFalse(TemplateSearch.matches(template, "pharmacy"))
    }

    @Test
    fun trimsTheQuery() {
        assertTrue(TemplateSearch.matches(template, "  please  "))
    }

    @Test
    fun foldRemovesAccentsAndCase() {
        assertEquals("cafe", TemplateSearch.fold("  CAFÉ "))
    }
}
