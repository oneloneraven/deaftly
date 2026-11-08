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

    public fun quote(messageId: Int): Quote {
        val message = requireNotNull(messages.getOrNull(messageId)) { "No message with id $messageId" }
        return Quote(message.id, message.plainText)
    }

    public fun append(
        spans: List<Span>,
        quoting: Int? = null,
    ): Conversation {
        require(spans.isNotEmpty()) { "A message must not be empty" }
        val message = Message(messages.size, turn, spans, quoting?.let(::quote))
        return copy(messages = messages + message)
    }
}
