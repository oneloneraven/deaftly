package com.ravendarque.deaftly.ui.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatInsertions {
    private val waiting = MutableStateFlow<String?>(null)

    val text: StateFlow<String?> = waiting

    fun offer(text: String) {
        waiting.value = text
    }

    fun consume() {
        waiting.value = null
    }
}
