package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val TEXT_SIZE_KEY = "text_size"

class ChatDisplay(
    private val savedState: SavedStateHandle,
) {
    private val size = MutableStateFlow(ChatTextSize.entries.getOrElse(savedState.get<Int>(TEXT_SIZE_KEY) ?: 0) { ChatTextSize.Normal })

    val textSize: StateFlow<ChatTextSize> = size

    fun cycleTextSize() {
        size.update { it.next() }
        savedState[TEXT_SIZE_KEY] = size.value.ordinal
    }
}
