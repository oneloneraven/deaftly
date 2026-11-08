package com.ravendarque.deaftly.core.chat

public enum class ParticipantId { A, B }

public enum class ColourRole { Accent, PersonB }

public data class Participant(
    val id: ParticipantId,
    val colour: ColourRole,
    val name: String? = null,
) {
    public val displayName: String? get() = name?.trim()?.takeIf { it.isNotEmpty() }

    public val avatarInitial: String?
        get() =
            displayName?.let { trimmed ->
                trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).uppercase()
            }

    public companion object {
        public fun defaultA(name: String? = null): Participant = Participant(ParticipantId.A, ColourRole.Accent, name)

        public fun defaultB(name: String? = null): Participant = Participant(ParticipantId.B, ColourRole.PersonB, name)
    }
}
