package com.ravendarque.deaftly.core.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TemplateFillTest {
    @Test
    fun startsEmpty() {
        val fill = TemplateFill()
        assertEquals(emptyList(), fill.values("a"))
        assertEquals(null, fill.value("a"))
    }

    @Test
    fun setReplacesWithASingleValue() {
        val fill = TemplateFill().set("a", "one").set("a", "two")
        assertEquals(listOf("two"), fill.values("a"))
        assertEquals("two", fill.value("a"))
    }

    @Test
    fun addAppendsInOrder() {
        val fill = TemplateFill().add("a", "one").add("a", "two").add("a", "three")
        assertEquals(listOf("one", "two", "three"), fill.values("a"))
        assertEquals("one", fill.value("a"))
    }

    @Test
    fun removeAtDropsThatIndexOnly() {
        val fill =
            TemplateFill()
                .add("a", "one")
                .add("a", "two")
                .add("a", "three")
                .removeAt("a", 1)
        assertEquals(listOf("one", "three"), fill.values("a"))
    }

    @Test
    fun removingTheLastValueClearsTheKey() {
        val fill = TemplateFill().set("a", "one").removeAt("a", 0)
        assertEquals(emptyList(), fill.values("a"))
        assertEquals(TemplateFill(), fill)
    }

    @Test
    fun removeAtRejectsBadIndex() {
        val fill = TemplateFill().add("a", "one")
        assertFailsWith<IndexOutOfBoundsException> { fill.removeAt("a", 1) }
        assertFailsWith<IndexOutOfBoundsException> { fill.removeAt("a", -1) }
        assertFailsWith<IndexOutOfBoundsException> { fill.removeAt("missing", 0) }
    }

    @Test
    fun clearRemovesOnlyThatKey() {
        val fill = TemplateFill().set("a", "one").set("b", "two").clear("a")
        assertEquals(emptyList(), fill.values("a"))
        assertEquals(listOf("two"), fill.values("b"))
    }

    @Test
    fun clearOfUnknownKeyIsANoOp() {
        val fill = TemplateFill().set("a", "one")
        assertEquals(fill, fill.clear("missing"))
    }

    @Test
    fun operationsDoNotMutateTheOriginal() {
        val original = TemplateFill().set("a", "one")
        original.add("a", "two")
        original.set("a", "three")
        original.clear("a")
        assertEquals(listOf("one"), original.values("a"))
    }
}
