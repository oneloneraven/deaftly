package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.FormattedText
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange
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

    @Test
    fun roundTripsADraftWithFormattingSelectionPendingStylesAndQuote() {
        val draft =
            Draft(
                Composition(FormattedText("Hello world", listOf(StyleRange(0, 5, SpanStyle.Bold))), 2, 7, setOf(SpanStyle.Highlight)),
                Quote(3, "spicy or mild?"),
            )

        assertEquals(draft, ChatCodec.decodeDraft(ChatCodec.encodeDraft(draft)))
    }

    @Test
    fun anEmptyDraftRoundTrips() {
        assertEquals(Draft(), ChatCodec.decodeDraft(ChatCodec.encodeDraft(Draft())))
    }

    @Test
    fun anUnreadableDraftDecodesToAnEmptyOne() {
        assertEquals(Draft(), ChatCodec.decodeDraft(null))
        assertEquals(Draft(), ChatCodec.decodeDraft("not json"))
        assertEquals(Draft(), ChatCodec.decodeDraft("""{"text":"ab","ranges":[{"start":0,"end":9,"style":"Bold"}]}"""))
    }
}
