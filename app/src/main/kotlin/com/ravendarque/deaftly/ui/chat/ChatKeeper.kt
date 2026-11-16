package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.storage.SavedChatStore
import com.ravendarque.deaftly.storage.logKeptChatSaveFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val KEPT_ID_KEY = "keptChatId"

class ChatKeeper(
    private val store: SavedChatStore,
    private val savedState: SavedStateHandle,
    private val writeScope: CoroutineScope,
) {
    private val writes = Mutex()

    val keptId: StateFlow<String?> = savedState.getStateFlow(KEPT_ID_KEY, null)

    fun keep(conversation: Conversation) {
        val id = keptId.value ?: store.newId().also { savedState[KEPT_ID_KEY] = it }
        write(id, conversation)
    }

    fun persist(conversation: Conversation) {
        keptId.value?.let { write(it, conversation) }
    }

    suspend fun open(id: String): Conversation? = store.load(id)?.conversation?.also { savedState[KEPT_ID_KEY] = id }

    private fun write(
        id: String,
        conversation: Conversation,
    ) {
        writeScope.launch(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock { runCatching { store.save(id, conversation) }.onFailure(::logKeptChatSaveFailure) }
        }
    }
}
