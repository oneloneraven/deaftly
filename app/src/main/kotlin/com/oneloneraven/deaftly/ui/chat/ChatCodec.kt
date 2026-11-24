package com.oneloneraven.deaftly.ui.chat

import com.oneloneraven.deaftly.core.chat.ColourRole
import com.oneloneraven.deaftly.core.chat.Composition
import com.oneloneraven.deaftly.core.chat.Conversation
import com.oneloneraven.deaftly.core.chat.FormattedText
import com.oneloneraven.deaftly.core.chat.Message
import com.oneloneraven.deaftly.core.chat.Participant
import com.oneloneraven.deaftly.core.chat.ParticipantId
import com.oneloneraven.deaftly.core.chat.Quote
import com.oneloneraven.deaftly.core.chat.Span
import com.oneloneraven.deaftly.core.chat.SpanStyle
import com.oneloneraven.deaftly.core.chat.StyleRange
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ConversationDto(
    val a: ParticipantDto,
    val b: ParticipantDto,
    val turn: ParticipantId,
    val messages: List<MessageDto>,
)

@Serializable
internal data class ParticipantDto(
    val colour: ColourRole,
    val name: String? = null,
)

@Serializable
internal data class MessageDto(
    val author: ParticipantId,
    val spans: List<SpanDto>,
    val quoteId: Int? = null,
    val quoteText: String? = null,
    val voiceTyped: Boolean = false,
    val edited: Boolean = false,
)

@Serializable
internal data class SpanDto(
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
    val editingId: Int? = null,
    val resume: DraftDto? = null,
)

@Serializable
private data class RangeDto(
    val start: Int,
    val end: Int,
    val style: SpanStyle,
)

internal object ChatCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(conversation: Conversation): String = json.encodeToString(ConversationDto.serializer(), conversation.toDto())

    fun decode(encoded: String?): Conversation? =
        encoded?.let { runCatching { json.decodeFromString(ConversationDto.serializer(), it).toDomain() }.getOrNull() }

    fun encodeDraft(draft: Draft): String = json.encodeToString(DraftDto.serializer(), draft.toDto())

    fun decodeDraft(encoded: String?): Draft =
        encoded?.let { runCatching { json.decodeFromString(DraftDto.serializer(), it).toDomain() }.getOrNull() } ?: Draft()
}

private fun Draft.toDto(): DraftDto =
    DraftDto(
        text = composition.text,
        ranges = composition.content.ranges.map { RangeDto(it.start, it.end, it.style) },
        selectionStart = composition.selectionStart,
        selectionEnd = composition.selectionEnd,
        typing = composition.typing,
        quoteId = quote?.messageId,
        quoteText = quote?.text,
        editingId = editing?.messageId,
        resume = editing?.resume?.toDto(),
    )

private fun DraftDto.toDomain(): Draft {
    val content = FormattedText(text, ranges.map { range -> StyleRange(range.start, range.end, range.style) })
    return Draft(
        composition = Composition(content, selectionStart, selectionEnd, typing),
        quote = quoteText?.let { Quote(quoteId, it) },
        editing = editingId?.let { id -> resume?.let { Editing(id, it.toDomain()) } },
    )
}

internal fun Conversation.toDto() =
    ConversationDto(
        a = participantA.toDto(),
        b = participantB.toDto(),
        turn = turn,
        messages = messages.map { it.toDto() },
    )

internal fun ConversationDto.toDomain() =
    Conversation(
        participantA = Participant(ParticipantId.A, a.colour, a.name),
        participantB = Participant(ParticipantId.B, b.colour, b.name),
        messages = messages.mapIndexed { index, message -> message.toDomain(index) },
        turn = turn,
    )

private fun Participant.toDto() = ParticipantDto(colour, name)

private fun Message.toDto() =
    MessageDto(
        author,
        spans.map {
            SpanDto(it.text, it.styles)
        },
        quote?.messageId,
        quote?.text,
        voiceTyped,
        edited,
    )

private fun MessageDto.toDomain(id: Int) =
    Message(
        id = id,
        author = author,
        spans = spans.map { Span(it.text, it.styles) },
        quote = quoteText?.let { Quote(quoteId, it) },
        voiceTyped = voiceTyped,
        edited = edited,
    )
