package com.oneloneraven.deaftly.ui.chat

import com.oneloneraven.deaftly.core.settings.ChatTextSize
import com.oneloneraven.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatDisplay(
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    val textSize: Flow<ChatTextSize> = settings.chatTextSize

    val sidesSwapped: Flow<Boolean> = settings.chatSidesSwapped

    fun cycleTextSize() {
        scope.launch { settings.setChatTextSize(settings.chatTextSize.first().next()) }
    }

    fun switchSides() {
        scope.launch { settings.setChatSidesSwapped(!settings.chatSidesSwapped.first()) }
    }
}
