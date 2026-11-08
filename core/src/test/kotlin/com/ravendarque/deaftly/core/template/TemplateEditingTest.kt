package com.ravendarque.deaftly.core.template

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemplateEditingTest {
    private val body = "Hi {name}, could I have {menu item} please?"

    @Test
    fun tokensCarryTheirOffsets() {
        assertEquals(
            listOf(PlaceholderToken("name", 3, 9), PlaceholderToken("menu item", 24, 35)),
            TemplateParser.tokens(body),
        )
    }

    @Test
    fun escapedBracesAreNotTokens() {
        assertEquals(emptyList(), TemplateParser.tokens("a {{b}} c"))
        assertEquals(listOf(PlaceholderToken("x", 4, 7)), TemplateParser.tokens("{{a}{x}"))
    }

    @Test
    fun keysAreDistinctInOrder() {
        assertEquals(listOf("b", "a"), TemplateEditing.keys("{b} {a} {b}"))
    }

    @Test
    fun aCursorInsideOrAtTheEndOfATokenFindsIt() {
        assertEquals("name", TemplateEditing.tokenAt(body, 5)?.key)
        assertEquals("name", TemplateEditing.tokenAt(body, 9)?.key)
        assertNull(TemplateEditing.tokenAt(body, 3))
        assertNull(TemplateEditing.tokenAt(body, 10))
    }

    @Test
    fun insertingATokenReplacesTheSelectionAndPlacesTheCursorAfterIt() {
        assertEquals(TextEdit("Hi {x}!", 6), TemplateEditing.insertToken("Hi !", 3, 3, "x"))
        assertEquals(TextEdit("Hi {x}", 6), TemplateEditing.insertToken("Hi there", 8, 3, "x"))
        assertEquals(TextEdit("{x}", 3), TemplateEditing.insertToken("", 0, 0, "x"))
    }

    @Test
    fun renamingChangesEveryTokenWithThatKey() {
        val rename = TemplateEditing.renameKey("{a} and {a} and {b}", "a", "longer")

        assertEquals("{longer} and {longer} and {b}", rename.text)
    }

    @Test
    fun renamingMapsOffsetsBeforeInsideAndAfterTokens() {
        val rename = TemplateEditing.renameKey("{a} x {a} y", "a", "long")

        assertEquals("{long} x {long} y", rename.text)
        assertEquals(0, rename.mapOffset(0))
        assertEquals(6, rename.mapOffset(2))
        assertEquals(6, rename.mapOffset(3))
        assertEquals(7, rename.mapOffset(4))
        assertEquals(rename.text.length, rename.mapOffset(11))
    }

    @Test
    fun removingATokenDropsOneOfTheSpacesAroundIt() {
        assertEquals("Hi there", TemplateEditing.removeKey("Hi {x} there", "x"))
        assertEquals("Hi there", TemplateEditing.removeKey("Hi {x} there {x}", "x").trim())
        assertEquals("keep {y}", TemplateEditing.removeKey("keep {y}{x}", "x"))
    }

    @Test
    fun nextFreeKeyAddsANumberWhenTheBaseIsTaken() {
        assertEquals("new placeholder", TemplateEditing.nextFreeKey("new placeholder", emptyList()))
        assertEquals("new placeholder 2", TemplateEditing.nextFreeKey("new placeholder", listOf("new placeholder")))
        assertEquals(
            "new placeholder 3",
            TemplateEditing.nextFreeKey("new placeholder", listOf("new placeholder", "new placeholder 2")),
        )
    }

    @Test
    fun deletingACharacterOfATokenRemovesTheWholeToken() {
        assertEquals(TextEdit("Hi , could", 3), TemplateEditing.keepTokensWhole("Hi {name}, could", "Hi {nam}, could"))
        assertEquals(TextEdit("Hi , could", 3), TemplateEditing.keepTokensWhole("Hi {name}, could", "Hi name}, could"))
        assertEquals(TextEdit("Hi , could", 3), TemplateEditing.keepTokensWhole("Hi {name}, could", "Hi {name, could"))
    }

    @Test
    fun deletingAcrossTwoTokensRemovesBoth() {
        assertEquals(TextEdit("A", 1), TemplateEditing.keepTokensWhole("A{b}{c}", "A{b"))
    }

    @Test
    fun deletingAWholeTokenOrPlainTextIsLeftAlone() {
        assertNull(TemplateEditing.keepTokensWhole("Hi {name}, ok", "Hi , ok"))
        assertNull(TemplateEditing.keepTokensWhole("Hi {name}, ok", "Hi {name}, o"))
        assertNull(TemplateEditing.keepTokensWhole("same", "same"))
    }

    @Test
    fun typingInsideATokenMovesTheTextAfterIt() {
        assertEquals(TextEdit("Hi {name}x, ok", 10), TemplateEditing.keepTokensWhole("Hi {name}, ok", "Hi {naxme}, ok"))
    }

    @Test
    fun typingAtTheEdgesOfATokenIsLeftAlone() {
        assertNull(TemplateEditing.keepTokensWhole("Hi {name}, ok", "Hi x{name}, ok"))
        assertNull(TemplateEditing.keepTokensWhole("Hi {name}, ok", "Hi {name}x, ok"))
    }

    @Test
    fun overtypingPartOfATokenReplacesTheWholeToken() {
        assertEquals(TextEdit("Hi Z, ok", 4), TemplateEditing.keepTokensWhole("Hi {name}, ok", "Hi {Z, ok"))
    }
}
