package com.ravendarque.deaftly.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class BuiltInFingerprintTest {
    private val dto = greeting.toDto()

    private fun TemplateDto.fillingNameWith(field: PersonalFieldDto?) =
        copy(placeholders = placeholders.map { if (it.key == "name") it.copy(fillsWith = field) else it })

    @Test
    fun aPlaceholderThatFillsWithNothingFingerprintsAsItAlwaysHas() {
        assertEquals(dto.contentFingerprint(), dto.fillingNameWith(null).contentFingerprint())
    }

    @Test
    fun eachPersonalFieldFingerprintsDifferently() {
        val fingerprints = (listOf(null) + PersonalFieldDto.entries).map { dto.fillingNameWith(it).contentFingerprint() }

        assertEquals(fingerprints.size, fingerprints.toSet().size)
    }
}
