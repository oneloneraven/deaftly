package com.oneloneraven.deaftly.ui.chat

import com.oneloneraven.deaftly.core.chat.Composition
import com.oneloneraven.deaftly.core.chat.Conversation
import com.oneloneraven.deaftly.core.chat.FormattedText
import com.oneloneraven.deaftly.core.chat.Participant
import com.oneloneraven.deaftly.core.chat.Quote
import com.oneloneraven.deaftly.core.chat.RichText
import com.oneloneraven.deaftly.core.chat.SpanStyle
import com.oneloneraven.deaftly.core.chat.StyleRange
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
    fun voiceTypedSurvivesARoundTrip() {
        val conversation = Conversation(Participant.defaultA(), Participant.defaultB()).appendVoiceTyped("Tuesday")

        assertEquals(conversation, ChatCodec.decode(ChatCodec.encode(conversation)))
    }

    @Test
    fun messagesWithoutTheFieldReadAsTyped() {
        val encoded =
            """{"a":{"colour":"Red"},"b":{"colour":"Blue"},"turn":"A","messages":[{"author":"A","spans":[{"text":"Hi"}]}]}"""

        assertEquals(
            false,
            ChatCodec
                .decode(encoded)
                ?.messages
                ?.single()
                ?.voiceTyped,
        )
    }

    @Test
    fun editedSurvivesARoundTrip() {
        val conversation =
            Conversation(Participant.defaultA(), Participant.defaultB())
                .append(RichText.spans("Tuesday"))
                .replaceMessage(0, RichText.spans("Wednesday"), null)

        assertEquals(conversation, ChatCodec.decode(ChatCodec.encode(conversation)))
        assertEquals(
            true,
            ChatCodec
                .decode(ChatCodec.encode(conversation))
                ?.messages
                ?.single()
                ?.edited,
        )
    }

    @Test
    fun messagesWithoutEditedReadAsUnedited() {
        val encoded =
            """{"a":{"colour":"Red"},"b":{"colour":"Blue"},"turn":"A","messages":[{"author":"A","spans":[{"text":"Hi"}]}]}"""

        assertEquals(
            false,
            ChatCodec
                .decode(encoded)
                ?.messages
                ?.single()
                ?.edited,
        )
    }

    @Test
    fun anEditingDraftWithItsSetAsideDraftSurvivesARoundTrip() {
        val resume =
            Draft(
                Composition(FormattedText("Half typed", listOf(StyleRange(0, 4, SpanStyle.Italic))), 4, 4),
                Quote(0, "Hello"),
            )
        val draft =
            Draft(
                Composition(FormattedText("Wednesday", listOf(StyleRange(0, 3, SpanStyle.Bold)))),
                Quote(1, "Tuesday"),
                Editing(2, resume),
            )

        assertEquals(draft, ChatCodec.decodeDraft(ChatCodec.encodeDraft(draft)))
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
