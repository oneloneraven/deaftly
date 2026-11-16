package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.storage.SavedChatStore
import com.ravendarque.deaftly.storage.logKeptChatSaveFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

private const val KEPT_ID_KEY = "keptChatId"

class ChatKeeper(
    private val store: SavedChatStore,
    private val savedState: SavedStateHandle,
    private val writeScope: CoroutineScope,
) {
    private val writes = Mutex()

    val keptId: StateFlow<String?> = savedState.getStateFlow(KEPT_ID_KEY, null)

    suspend fun keep(conversation: Conversation): Boolean {
        val id = keptId.value ?: store.newId()
        val saved = write(id, conversation).await()
        if (saved) savedState[KEPT_ID_KEY] = id
        return saved
    }

    fun persist(conversation: Conversation) {
        keptId.value?.let { write(it, conversation) }
    }

    suspend fun open(id: String): Conversation? = store.load(id)?.conversation?.also { savedState[KEPT_ID_KEY] = id }

    private fun write(
        id: String,
        conversation: Conversation,
    ): Deferred<Boolean> =
        writeScope.async(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock {
                runCatching { store.save(id, conversation) }
                    .onFailure { if (it is CancellationException) throw it else logKeptChatSaveFailure(it) }
                    .isSuccess
            }
        }
}
