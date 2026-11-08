package com.ravendarque.deaftly.core.settings

import com.ravendarque.deaftly.core.chat.ChatProfile
import kotlinx.coroutines.flow.Flow

public interface SettingsRepository {
    public val showScheme: Flow<ShowScheme?>

    public val chatProfile: Flow<ChatProfile>

    public val chatLayout: Flow<ChatLayout>

    public val firstRunDone: Flow<Boolean>

    public suspend fun setShowScheme(scheme: ShowScheme)

    public suspend fun setChatProfile(profile: ChatProfile)

    public suspend fun setChatLayout(layout: ChatLayout)

    public suspend fun setFirstRunDone()
}
