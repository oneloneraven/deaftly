package com.ravendarque.deaftly.core.chat

public data class ChatProfile(
    val name: String? = null,
    val colour: ColourRole = ColourRole.Indigo,
) {
    public fun startConversation(): Conversation {
        val a = Participant(ParticipantId.A, colour, name)
        val b = Participant.defaultB().let { if (it.colour == colour) it.copy(colour = firstColourOtherThan(colour)) else it }
        return Conversation(a, b)
    }

    private fun firstColourOtherThan(taken: ColourRole): ColourRole = ColourRole.entries.first { it != taken }
}
