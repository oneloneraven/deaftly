package com.oneloneraven.deaftly.ui.savedchats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneloneraven.deaftly.storage.SavedChatStore
import com.oneloneraven.deaftly.storage.SavedChatSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedChatsViewModel(
    private val store: SavedChatStore,
    private val writeScope: CoroutineScope,
) : ViewModel() {
    val chats: StateFlow<List<SavedChatSummary>> = store.summaries

    val loaded: StateFlow<Boolean> = store.loaded

    private val notOpened = MutableStateFlow(false)

    val couldNotOpen: StateFlow<Boolean> = notOpened.asStateFlow()

    fun open(
        id: String,
        onOpen: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val found = store.load(id) != null
            notOpened.value = !found
            if (found) onOpen(id)
        }
    }

    fun delete(id: String) {
        notOpened.value = false
        writeScope.launch { store.delete(id) }
    }
}
