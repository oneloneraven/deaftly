package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatProfileTest {
    @Test
    fun defaultProfileStartsWithTheAccentAndPersonBColours() {
        val conversation = ChatProfile().startConversation()
        assertEquals(ColourRole.Indigo, conversation.participantA.colour)
        assertEquals(ColourRole.Orange, conversation.participantB.colour)
        assertNull(conversation.participantA.name)
        assertNull(conversation.participantB.name)
    }

    @Test
    fun savedNameAndColourAreAppliedToTheDeafUser() {
        val conversation = ChatProfile(name = "Alex", colour = ColourRole.Green).startConversation()
        assertEquals(Participant(ParticipantId.A, ColourRole.Green, "Alex"), conversation.participantA)
        assertEquals(ColourRole.Orange, conversation.participantB.colour)
    }

    @Test
    fun theOtherPersonGivesWayWhenTheSavedColourIsTheirDefault() {
        val conversation = ChatProfile(colour = ColourRole.Orange).startConversation()
        assertEquals(ColourRole.Orange, conversation.participantA.colour)
        assertEquals(ColourRole.Red, conversation.participantB.colour)
    }

    @Test
    fun theOtherPersonStartsUnnamed() {
        assertNull(ChatProfile(name = "Alex").startConversation().participantB.name)
    }
}
