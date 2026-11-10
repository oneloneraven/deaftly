package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.FormattedText
import com.ravendarque.deaftly.core.chat.Message
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.core.chat.StyleRange
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

@Serializable
private data class DraftDto(
    val text: String = "",
    val ranges: List<RangeDto> = emptyList(),
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val typing: Set<SpanStyle>? = null,
    val quoteId: Int? = null,
    val quoteText: String? = null,
)

@Serializable
private data class RangeDto(
    val start: Int,
    val end: Int,
    val style: SpanStyle,
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

    fun encodeDraft(draft: Draft): String =
        json.encodeToString(
            DraftDto.serializer(),
            DraftDto(
                text = draft.composition.text,
                ranges =
                    draft.composition.content.ranges
                        .map { RangeDto(it.start, it.end, it.style) },
                selectionStart = draft.composition.selectionStart,
                selectionEnd = draft.composition.selectionEnd,
                typing = draft.composition.typing,
                quoteId = draft.quote?.messageId,
                quoteText = draft.quote?.text,
            ),
        )

    fun decodeDraft(encoded: String?): Draft =
        encoded
            ?.let {
                runCatching {
                    val dto = json.decodeFromString(DraftDto.serializer(), it)
                    val content = FormattedText(dto.text, dto.ranges.map { range -> StyleRange(range.start, range.end, range.style) })
                    Draft(
                        composition = Composition(content, dto.selectionStart, dto.selectionEnd, dto.typing),
                        quote = dto.quoteText?.let { Quote(dto.quoteId, it) },
                    )
                }.getOrNull()
            } ?: Draft()

    private fun Participant.toDto() = ParticipantDto(colour, name)

    private fun Message.toDto() = MessageDto(author, spans.map { SpanDto(it.text, it.styles) }, quote?.messageId, quote?.text)

    private fun MessageDto.toDomain(id: Int) =
        Message(
            id = id,
            author = author,
            spans = spans.map { Span(it.text, it.styles) },
            quote = quoteText?.let { Quote(quoteId, it) },
        )
}
