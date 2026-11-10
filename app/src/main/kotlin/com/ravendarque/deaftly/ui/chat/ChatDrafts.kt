package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.FormattedText
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val DRAFT_A_KEY = "draft_a"
private const val DRAFT_B_KEY = "draft_b"

class ChatDrafts(
    private val savedState: SavedStateHandle,
    private val turn: () -> ParticipantId?,
) {
    private val drafts =
        MutableStateFlow(
            mapOf(
                ParticipantId.A to ChatCodec.decodeDraft(savedState[DRAFT_A_KEY]),
                ParticipantId.B to ChatCodec.decodeDraft(savedState[DRAFT_B_KEY]),
            ),
        )

    val all: StateFlow<Map<ParticipantId, Draft>> = drafts

    fun setComposition(composition: Composition) {
        update { it.copy(composition = composition) }
    }

    fun insertText(text: String) {
        update { it.copy(composition = it.composition.insert(text)) }
    }

    fun setQuote(quote: Quote?) {
        update { it.copy(quote = quote) }
    }

    fun afterDeleting(messageId: Int) {
        drafts.update { all ->
            all.mapValues { (_, draft) ->
                val quote = draft.quote
                val quoted = quote?.messageId
                when {
                    quote == null || quoted == null || quoted < messageId -> draft
                    quoted == messageId -> draft.copy(quote = null)
                    else -> draft.copy(quote = quote.copy(messageId = quoted - 1))
                }
            }
        }
        ParticipantId.entries.forEach(::persist)
    }

    fun restore(
        owner: ParticipantId,
        text: String,
    ) {
        if (text.isBlank()) return
        drafts.update { it + (owner to Draft(Composition(FormattedText(text)))) }
        persist(owner)
    }

    fun clear() {
        update { Draft() }
    }

    private fun update(transform: (Draft) -> Draft) {
        val owner = turn() ?: return
        drafts.update { it + (owner to transform(it.getValue(owner))) }
        persist(owner)
    }

    private fun persist(owner: ParticipantId) {
        savedState[if (owner == ParticipantId.A) DRAFT_A_KEY else DRAFT_B_KEY] = ChatCodec.encodeDraft(drafts.value.getValue(owner))
    }
}
