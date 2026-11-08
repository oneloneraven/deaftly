package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatProfileTest {
    @Test
    fun defaultProfileStartsWithTheAccentAndPersonBColours() {
        val conversation = ChatProfile().startConversation()
        assertEquals(ColourRole.Accent, conversation.participantA.colour)
        assertEquals(ColourRole.PersonB, conversation.participantB.colour)
        assertNull(conversation.participantA.name)
        assertNull(conversation.participantB.name)
    }

    @Test
    fun savedNameAndColourAreAppliedToTheDeafUser() {
        val conversation = ChatProfile(name = "Alex", colour = ColourRole.Teal).startConversation()
        assertEquals(Participant(ParticipantId.A, ColourRole.Teal, "Alex"), conversation.participantA)
        assertEquals(ColourRole.PersonB, conversation.participantB.colour)
    }

    @Test
    fun theOtherPersonGivesWayWhenTheSavedColourIsTheirDefault() {
        val conversation = ChatProfile(colour = ColourRole.PersonB).startConversation()
        assertEquals(ColourRole.PersonB, conversation.participantA.colour)
        assertEquals(ColourRole.Accent, conversation.participantB.colour)
    }

    @Test
    fun theOtherPersonStartsUnnamed() {
        assertNull(ChatProfile(name = "Alex").startConversation().participantB.name)
    }
}
