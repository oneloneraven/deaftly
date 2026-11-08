package com.ravendarque.deaftly.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConversationTest {
    private fun conversation() = Conversation(Participant.defaultA("Me"), Participant.defaultB("Them"))

    private fun text(value: String) = RichText.spans(value)

    @Test
    fun startsEmptyOnTheDeafUsersTurn() {
        val conversation = conversation()
        assertTrue(conversation.messages.isEmpty())
        assertEquals(ParticipantId.A, conversation.turn)
        assertEquals("Me", conversation.activeParticipant.name)
    }

    @Test
    fun appendAddsAMessageByTheActiveParticipant() {
        val conversation = conversation().append(text("Hello"))
        val message = conversation.messages.single()
        assertEquals(0, message.id)
        assertEquals(ParticipantId.A, message.author)
        assertEquals("Hello", message.plainText)
        assertNull(message.quote)
    }

    @Test
    fun appendDoesNotSwitchTheTurn() {
        assertEquals(ParticipantId.A, conversation().append(text("Hello")).turn)
    }

    @Test
    fun messagesKeepTheirOrderAndAuthors() {
        val conversation =
            conversation()
                .append(text("one"))
                .switchTurn()
                .append(text("two"))
                .append(text("three"))
                .switchTurn()
                .append(text("four"))
        assertEquals(listOf("one", "two", "three", "four"), conversation.messages.map { it.plainText })
        assertEquals(
            listOf(ParticipantId.A, ParticipantId.B, ParticipantId.B, ParticipantId.A),
            conversation.messages.map { it.author },
        )
        assertEquals(listOf(0, 1, 2, 3), conversation.messages.map { it.id })
    }

    @Test
    fun switchTurnAlternatesAndIsReversible() {
        val conversation = conversation()
        assertEquals(ParticipantId.B, conversation.switchTurn().turn)
        assertEquals(ParticipantId.A, conversation.switchTurn().switchTurn().turn)
    }

    @Test
    fun withTurnSetsTheTurnExplicitly() {
        val conversation = conversation().withTurn(ParticipantId.B)
        assertEquals(ParticipantId.B, conversation.turn)
        assertEquals(ParticipantId.B, conversation.withTurn(ParticipantId.B).turn)
        assertEquals("Them", conversation.activeParticipant.name)
    }

    @Test
    fun operationsReturnNewConversationsAndLeaveTheOriginalUntouched() {
        val original = conversation()
        original.append(text("Hello"))
        original.switchTurn()
        assertTrue(original.messages.isEmpty())
        assertEquals(ParticipantId.A, original.turn)
    }

    @Test
    fun quotingSnapshotsTheEarlierMessageAsPlainText() {
        val styled = RichText.spans("Hello brave world", listOf(StyleRange(6, 11, SpanStyle.Bold)))
        val conversation =
            conversation()
                .append(styled)
                .switchTurn()
                .let { it.append(text("What?"), it.quote(0)) }
        val quote = conversation.messages.last().quote
        assertEquals(Quote(messageId = 0, text = "Hello brave world"), quote)
    }

    @Test
    fun quoteTextIsASnapshotAndQuotesDoNotNest() {
        val first = conversation().append(text("first"))
        val second = first.append(text("second"), first.quote(0))
        val conversation = second.append(text("third"), second.quote(1))
        assertEquals("second", conversation.messages[2].quote?.text)
    }

    @Test
    fun quotingAnExcerptKeepsJustThatText() {
        val conversation = conversation().append(text("Do you want it spicy or mild?"))
        assertEquals(Quote(0, "spicy or mild"), conversation.quote(0, "spicy or mild"))
        assertEquals(Quote(0, "Do you want it spicy or mild?"), conversation.quote(0, "  "))
    }

    @Test
    fun quotingAnUnknownMessageIsRejected() {
        assertFailsWith<IllegalArgumentException> { conversation().quote(0) }
        assertFailsWith<IllegalArgumentException> { conversation().append(text("Hi"), Quote(3, "x")) }
        assertFailsWith<IllegalArgumentException> { conversation().append(text("Hi")).quote(-1) }
    }

    @Test
    fun emptyMessageIsRejected() {
        assertFailsWith<IllegalArgumentException> { conversation().append(emptyList()) }
    }

    @Test
    fun participantLookupById() {
        val conversation = conversation()
        assertSame(conversation.participantA, conversation.participant(ParticipantId.A))
        assertSame(conversation.participantB, conversation.participant(ParticipantId.B))
    }

    @Test
    fun participantsMustHoldTheirOwnSide() {
        assertFailsWith<IllegalArgumentException> {
            Conversation(Participant.defaultB(), Participant.defaultB())
        }
        assertFailsWith<IllegalArgumentException> {
            Conversation(Participant.defaultA(), Participant.defaultA())
        }
    }

    @Test
    fun messageIdsMustMatchTheirPosition() {
        assertFailsWith<IllegalArgumentException> {
            Conversation(
                Participant.defaultA(),
                Participant.defaultB(),
                messages = listOf(Message(1, ParticipantId.A, text("x"))),
            )
        }
    }

    @Test
    fun participantsMustHaveDifferentColours() {
        assertFailsWith<IllegalArgumentException> {
            Conversation(Participant.defaultA(), Participant.defaultB().copy(colour = ColourRole.Accent))
        }
    }

    @Test
    fun withParticipantReplacesOnlyThatSide() {
        val updated = conversation().withParticipant(Participant(ParticipantId.B, ColourRole.Teal, "Sam"))
        assertEquals("Me", updated.participantA.name)
        assertEquals("Sam", updated.participantB.name)
        assertEquals(ColourRole.Teal, updated.participantB.colour)
    }

    @Test
    fun withParticipantKeepsMessagesAndTurn() {
        val updated =
            conversation()
                .append(text("Hello"))
                .withTurn(ParticipantId.B)
                .withParticipant(Participant.defaultA("Alex"))
        assertEquals(1, updated.messages.size)
        assertEquals(ParticipantId.B, updated.turn)
    }

    @Test
    fun withParticipantRejectsTheOtherSidesColour() {
        assertFailsWith<IllegalArgumentException> {
            conversation().withParticipant(Participant(ParticipantId.A, ColourRole.PersonB))
        }
    }
}
