package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TEXT_SIZE_KEY = "text_size"
private const val LAYOUT_KEY = "layout"

class ChatDisplay(
    private val savedState: SavedStateHandle,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val size = MutableStateFlow(ChatTextSize.entries.getOrElse(savedState.get<Int>(TEXT_SIZE_KEY) ?: 0) { ChatTextSize.Normal })
    private val chosen = MutableStateFlow(ChatLayout.entries.getOrElse(savedState.get<Int>(LAYOUT_KEY) ?: -1) { ChatLayout.Sides })

    val textSize: StateFlow<ChatTextSize> = size
    val layout: StateFlow<ChatLayout> = chosen

    suspend fun restoreDefault() {
        if (!savedState.contains(LAYOUT_KEY)) apply(settings.chatLayout.first())
    }

    fun cycleTextSize() {
        size.update { it.next() }
        savedState[TEXT_SIZE_KEY] = size.value.ordinal
    }

    fun chooseLayout(layout: ChatLayout) {
        apply(layout)
        scope.launch { settings.setChatLayout(layout) }
    }

    private fun apply(layout: ChatLayout) {
        chosen.value = layout
        savedState[LAYOUT_KEY] = layout.ordinal
    }
}
