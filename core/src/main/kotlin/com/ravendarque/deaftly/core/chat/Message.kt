package com.ravendarque.deaftly.core.chat

public data class Quote(
    val messageId: Int?,
    val text: String,
)

public data class Message(
    val id: Int,
    val author: ParticipantId,
    val spans: List<Span>,
    val quote: Quote? = null,
) {
    public val plainText: String get() = RichText.plainText(spans)
}
