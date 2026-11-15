package com.ravendarque.deaftly.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BuiltInFingerprintTest {
    private val dto = greeting.toDto()

    private fun TemplateDto.fillingNameWith(field: PersonalFieldDto?) =
        copy(placeholders = placeholders.map { if (it.key == "name") it.copy(fillsWith = field) else it })

    @Test
    fun aPlaceholderThatFillsWithNothingFingerprintsAsItAlwaysHas() {
        assertEquals(dto.contentFingerprint(), dto.fillingNameWith(null).contentFingerprint())
    }

    @Test
    fun fillingWithAPersonalFieldChangesTheFingerprint() {
        assertNotEquals(dto.contentFingerprint(), dto.fillingNameWith(PersonalFieldDto.FormalName).contentFingerprint())
    }
}
