package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initial: ChatProfile = ChatProfile(),
) : SettingsRepository {
    private val current = MutableStateFlow(initial)

    override val chatProfile: Flow<ChatProfile> = current

    val saved: ChatProfile get() = current.value

    override suspend fun setChatProfile(profile: ChatProfile) {
        current.value = profile
    }
}
