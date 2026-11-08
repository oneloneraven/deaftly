package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatCodecTest {
    @Test
    fun roundTripsParticipantsTurnStyledMessagesAndQuotes() {
        val conversation = orderingConversation()

        assertEquals(conversation, ChatCodec.decode(ChatCodec.encode(conversation)))
    }

    @Test
    fun roundTripsAnEmptyConversationWithUnnamedParticipants() {
        val conversation = Conversation(Participant.defaultA(), Participant.defaultB())

        assertEquals(conversation, ChatCodec.decode(ChatCodec.encode(conversation)))
    }

    @Test
    fun anythingUnreadableDecodesToNothing() {
        assertNull(ChatCodec.decode(null))
        assertNull(ChatCodec.decode("not json"))
        assertNull(ChatCodec.decode("""{"a":{"colour":"Mauve"}}"""))
    }
}
