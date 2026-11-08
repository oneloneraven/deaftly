package com.ravendarque.deaftly.core.settings

import com.ravendarque.deaftly.core.chat.ChatProfile
import kotlinx.coroutines.flow.Flow

public interface SettingsRepository {
    public val chatProfile: Flow<ChatProfile>

    public suspend fun setChatProfile(profile: ChatProfile)
}
