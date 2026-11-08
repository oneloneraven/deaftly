package com.ravendarque.deaftly.core.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TemplateModelTest {
    @Test
    fun definedPlaceholderIsReturnedAsDefined() {
        val defined =
            Placeholder(
                key = "menu item",
                label = "What would you like?",
                type = PlaceholderType.Choices,
                options = listOf("Tea", "Coffee"),
                repeatable = true,
            )
        val template =
            Template(
                id = "order",
                title = "Order",
                category = "Food",
                body = "Could I have {menu item}?",
                placeholders = mapOf(defined.key to defined),
            )
        assertEquals(defined, template.placeholder("menu item"))
    }

    @Test
    fun undefinedKeyIsATypingPlaceholderLabelledByItsKey() {
        val template = Template(id = "t", title = "T", category = "c", body = "Hi {name}")
        assertEquals(
            Placeholder(key = "name", label = "name", type = PlaceholderType.Typing),
            template.placeholder("name"),
        )
    }

    @Test
    fun defaultsAreUnpinnedUserTemplates() {
        val template = Template(id = "t", title = "T", category = "c", body = "")
        assertEquals(false, template.pinned)
        assertEquals(false, template.builtIn)
        assertTrue(template.placeholders.isEmpty())
    }

    @Test
    fun placeholderMapKeyMustMatchPlaceholderKey() {
        assertFailsWith<IllegalArgumentException> {
            Template(
                id = "t",
                title = "T",
                category = "c",
                body = "",
                placeholders = mapOf("a" to Placeholder(key = "b", label = "B", type = PlaceholderType.Typing)),
            )
        }
    }

    @Test
    fun placeholderDefaultsToNoOptionsAndNotRepeatable() {
        val placeholder = Placeholder(key = "k", label = "L", type = PlaceholderType.Camera)
        assertTrue(placeholder.options.isEmpty())
        assertEquals(false, placeholder.repeatable)
    }
}
