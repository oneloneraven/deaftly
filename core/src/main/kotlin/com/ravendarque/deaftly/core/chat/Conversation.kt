package com.ravendarque.deaftly.core.chat

public data class Conversation(
    val participantA: Participant,
    val participantB: Participant,
    val messages: List<Message> = emptyList(),
    val turn: ParticipantId = ParticipantId.A,
) {
    init {
        require(participantA.id == ParticipantId.A) { "participantA must have id A" }
        require(participantB.id == ParticipantId.B) { "participantB must have id B" }
        require(participantA.colour != participantB.colour) { "Participants must have different colours" }
        require(messages.withIndex().all { (index, message) -> message.id == index }) {
            "Message ids must match their position in the conversation"
        }
    }

    public val activeParticipant: Participant get() = participant(turn)

    public fun participant(id: ParticipantId): Participant =
        when (id) {
            ParticipantId.A -> participantA
            ParticipantId.B -> participantB
        }

    public fun withParticipant(participant: Participant): Conversation =
        when (participant.id) {
            ParticipantId.A -> copy(participantA = participant)
            ParticipantId.B -> copy(participantB = participant)
        }

    public fun withTurn(turn: ParticipantId): Conversation = copy(turn = turn)

    public fun switchTurn(): Conversation = withTurn(if (turn == ParticipantId.A) ParticipantId.B else ParticipantId.A)

    public fun quote(
        messageId: Int,
        excerpt: String? = null,
    ): Quote {
        val message = requireNotNull(messages.getOrNull(messageId)) { "No message with id $messageId" }
        return Quote(message.id, excerpt?.takeIf { it.isNotBlank() } ?: message.plainText)
    }

    public fun append(
        spans: List<Span>,
        quote: Quote? = null,
    ): Conversation {
        require(spans.isNotEmpty()) { "A message must not be empty" }
        require(quote?.messageId == null || quote.messageId in messages.indices) { "No message with id ${quote?.messageId}" }
        val message = Message(messages.size, turn, spans, quote)
        return copy(messages = messages + message)
    }

    public fun deleteMessage(id: Int): Conversation {
        if (id !in messages.indices) return this
        val kept =
            messages.filterNot { it.id == id }.mapIndexed { index, message ->
                message.copy(id = index, quote = message.quote?.let { reindexed(it, id) })
            }
        return copy(messages = kept)
    }

    private fun reindexed(
        quote: Quote,
        deletedId: Int,
    ): Quote =
        when (val quoted = quote.messageId) {
            null -> quote
            deletedId -> quote.copy(messageId = null)
            else -> if (quoted < deletedId) quote else quote.copy(messageId = quoted - 1)
        }
}
