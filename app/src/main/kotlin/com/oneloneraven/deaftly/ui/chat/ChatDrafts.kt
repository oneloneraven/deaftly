package com.oneloneraven.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.oneloneraven.deaftly.core.chat.Composition
import com.oneloneraven.deaftly.core.chat.FormattedText
import com.oneloneraven.deaftly.core.chat.Message
import com.oneloneraven.deaftly.core.chat.ParticipantId
import com.oneloneraven.deaftly.core.chat.Quote
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

    fun startEdit(
        owner: ParticipantId,
        message: Message,
    ) {
        drafts.update { all ->
            val draft = all.getValue(owner)
            val resume = draft.editing?.resume ?: draft
            val loaded = Draft(Composition(FormattedText.of(message.spans)), message.quote, Editing(message.id, resume))
            all + (owner to loaded)
        }
        persist(owner)
    }

    fun cancelEdit() {
        update { it.editing?.resume ?: it }
    }

    fun setQuote(quote: Quote?) {
        update { it.copy(quote = quote) }
    }

    fun afterDeleting(messageId: Int) {
        drafts.update { all -> all.mapValues { (_, draft) -> draft.afterDeleting(messageId) } }
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
        update { it.editing?.resume ?: Draft() }
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

private fun Draft.afterDeleting(deletedId: Int): Draft {
    val editing = editing
    return when {
        editing?.messageId == deletedId -> {
            editing.resume.afterDeleting(deletedId)
        }

        else -> {
            copy(
                quote = quote.afterDeleting(deletedId, keepUnlinked = editing != null),
                editing =
                    editing?.copy(
                        messageId = editing.messageId.afterDeleting(deletedId),
                        resume = editing.resume.afterDeleting(deletedId),
                    ),
            )
        }
    }
}

private fun Int.afterDeleting(deletedId: Int): Int = if (this > deletedId) this - 1 else this

private fun Quote?.afterDeleting(
    deletedId: Int,
    keepUnlinked: Boolean,
): Quote? {
    val quoted = this?.messageId
    return when {
        this == null || quoted == null || quoted < deletedId -> this
        quoted == deletedId -> if (keepUnlinked) copy(messageId = null) else null
        else -> copy(messageId = quoted - 1)
    }
}

internal fun ChatDrafts.isEditing(
    owner: ParticipantId,
    messageId: Int,
): Boolean = all.value[owner]?.editing?.messageId == messageId
