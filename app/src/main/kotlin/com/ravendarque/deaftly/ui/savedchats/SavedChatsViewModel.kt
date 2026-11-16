package com.ravendarque.deaftly.ui.savedchats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.storage.SavedChatStore
import com.ravendarque.deaftly.storage.SavedChatSummary
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavedChatsViewModel(
    private val store: SavedChatStore,
) : ViewModel() {
    val chats: StateFlow<List<SavedChatSummary>> = store.summaries

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }
}
