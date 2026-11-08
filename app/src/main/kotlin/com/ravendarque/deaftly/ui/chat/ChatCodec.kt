package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Message
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ConversationDto(
    val a: ParticipantDto,
    val b: ParticipantDto,
    val turn: ParticipantId,
    val messages: List<MessageDto>,
)

@Serializable
private data class ParticipantDto(
    val colour: ColourRole,
    val name: String? = null,
)

@Serializable
private data class MessageDto(
    val author: ParticipantId,
    val spans: List<SpanDto>,
    val quoteId: Int? = null,
    val quoteText: String? = null,
)

@Serializable
private data class SpanDto(
    val text: String,
    val styles: Set<SpanStyle> = emptySet(),
)

internal object ChatCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(conversation: Conversation): String =
        json.encodeToString(
            ConversationDto.serializer(),
            ConversationDto(
                a = conversation.participantA.toDto(),
                b = conversation.participantB.toDto(),
                turn = conversation.turn,
                messages = conversation.messages.map { it.toDto() },
            ),
        )

    fun decode(encoded: String?): Conversation? =
        encoded?.let {
            runCatching {
                val dto = json.decodeFromString(ConversationDto.serializer(), it)
                Conversation(
                    participantA = Participant(ParticipantId.A, dto.a.colour, dto.a.name),
                    participantB = Participant(ParticipantId.B, dto.b.colour, dto.b.name),
                    messages = dto.messages.mapIndexed { index, message -> message.toDomain(index) },
                    turn = dto.turn,
                )
            }.getOrNull()
        }

    private fun Participant.toDto() = ParticipantDto(colour, name)

    private fun Message.toDto() = MessageDto(author, spans.map { SpanDto(it.text, it.styles) }, quote?.messageId, quote?.text)

    private fun MessageDto.toDomain(id: Int) =
        Message(
            id = id,
            author = author,
            spans = spans.map { Span(it.text, it.styles) },
            quote = if (quoteId != null && quoteText != null) Quote(quoteId, quoteText) else null,
        )
}
