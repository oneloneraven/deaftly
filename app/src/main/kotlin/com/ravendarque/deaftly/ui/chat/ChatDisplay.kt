package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.settings.ChatLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val TEXT_SIZE_KEY = "text_size"
private const val LAYOUT_KEY = "layout"

class ChatDisplay(
    private val savedState: SavedStateHandle,
    defaultLayout: ChatLayout = ChatLayout.Sides,
) {
    private val size = MutableStateFlow(ChatTextSize.entries.getOrElse(savedState.get<Int>(TEXT_SIZE_KEY) ?: 0) { ChatTextSize.Normal })
    private val chosen = MutableStateFlow(ChatLayout.entries.getOrElse(savedState.get<Int>(LAYOUT_KEY) ?: -1) { defaultLayout })

    val textSize: StateFlow<ChatTextSize> = size
    val layout: StateFlow<ChatLayout> = chosen

    fun cycleTextSize() {
        size.update { it.next() }
        savedState[TEXT_SIZE_KEY] = size.value.ordinal
    }

    fun setLayout(layout: ChatLayout) {
        chosen.value = layout
        savedState[LAYOUT_KEY] = layout.ordinal
    }
}
