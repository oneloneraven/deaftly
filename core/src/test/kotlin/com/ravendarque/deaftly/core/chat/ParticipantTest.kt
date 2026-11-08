package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParticipantTest {
    @Test
    fun defaultsUseTheAccentAndPersonBColours() {
        assertEquals(ColourRole.Accent, Participant.defaultA().colour)
        assertEquals(ColourRole.PersonB, Participant.defaultB().colour)
        assertEquals(ParticipantId.A, Participant.defaultA().id)
        assertEquals(ParticipantId.B, Participant.defaultB().id)
    }

    @Test
    fun nameIsOptional() {
        val participant = Participant.defaultA()
        assertNull(participant.name)
        assertNull(participant.displayName)
        assertNull(participant.avatarInitial)
    }

    @Test
    fun avatarInitialIsTheUppercasedFirstLetterOfTheName() {
        assertEquals("S", Participant.defaultB("sam").avatarInitial)
    }

    @Test
    fun blankNameHasNoInitial() {
        val participant = Participant.defaultB("   ")
        assertNull(participant.displayName)
        assertNull(participant.avatarInitial)
    }

    @Test
    fun displayNameIsTrimmed() {
        assertEquals("Sam", Participant.defaultB("  Sam ").displayName)
        assertEquals("S", Participant.defaultB("  sam ").avatarInitial)
    }

    @Test
    fun initialKeepsAWholeCodePoint() {
        assertEquals("😀", Participant.defaultA("😀 Grin").avatarInitial)
    }
}
