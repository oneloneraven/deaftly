package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.storage.SavedChat
import com.ravendarque.deaftly.storage.SavedChatStore
import com.ravendarque.deaftly.storage.SavedChatSummary
import com.ravendarque.deaftly.storage.isSavedChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.UUID

class FakeSavedChatStore(
    initial: List<SavedChat> = emptyList(),
    private val failSaves: Boolean = false,
) : SavedChatStore {
    private val chats = initial.associateBy { it.id }.toMutableMap()
    private val all = MutableStateFlow(summarise())
    private var clock = initial.maxOfOrNull { it.changedAt } ?: 0L

    var saves = 0
        private set

    override val summaries: StateFlow<List<SavedChatSummary>> = all

    override suspend fun load(id: String): SavedChat? = chats[id]

    override suspend fun save(
        id: String,
        conversation: Conversation,
    ) {
        require(isSavedChatId(id)) { "Not a saved chat ID" }
        if (failSaves) throw IOException("No space left")
        saves++
        chats[id] = SavedChat(id, conversation, ++clock)
        all.value = summarise()
    }

    override suspend fun delete(id: String) {
        chats.remove(id)
        all.value = summarise()
    }

    override fun newId(): String = UUID.randomUUID().toString()

    private fun summarise(): List<SavedChatSummary> =
        chats.values
            .sortedByDescending { it.changedAt }
            .map {
                SavedChatSummary(
                    it.id,
                    it.conversation.participantB.name,
                    it.conversation.messages
                        .lastOrNull()
                        ?.plainText,
                    it.changedAt,
                )
            }
}

val testWriteScope = CoroutineScope(Dispatchers.Unconfined)
