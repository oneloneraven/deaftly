package com.ravendarque.deaftly.core.settings

import com.ravendarque.deaftly.core.chat.ChatProfile
import kotlinx.coroutines.flow.Flow

public interface SettingsRepository {
    public val showScheme: Flow<ShowScheme?>

    public val chatProfile: Flow<ChatProfile>

    public val firstRunDone: Flow<Boolean>

    public val chatTextSize: Flow<ChatTextSize>

    public val chatSidesSwapped: Flow<Boolean>

    public val themeMode: Flow<ThemeMode>

    public suspend fun setShowScheme(scheme: ShowScheme)

    public suspend fun setChatProfile(profile: ChatProfile)

    public suspend fun setFirstRunDone()

    public suspend fun setChatTextSize(size: ChatTextSize)

    public suspend fun setChatSidesSwapped(swapped: Boolean)

    public suspend fun setThemeMode(mode: ThemeMode)
}
