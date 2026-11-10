package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.core.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initial: ChatProfile = ChatProfile(),
    showScheme: ShowScheme? = null,
    firstRunDone: Boolean = true,
    textSize: ChatTextSize = ChatTextSize.Default,
    sidesSwapped: Boolean = false,
    theme: ThemeMode = ThemeMode.System,
) : SettingsRepository {
    private val profile = MutableStateFlow(initial)
    private val scheme = MutableStateFlow(showScheme)
    private val firstRun = MutableStateFlow(firstRunDone)
    private val size = MutableStateFlow(textSize)
    private val swapped = MutableStateFlow(sidesSwapped)
    private val mode = MutableStateFlow(theme)

    override val chatProfile: Flow<ChatProfile> = profile

    override val showScheme: Flow<ShowScheme?> = scheme

    override val firstRunDone: Flow<Boolean> = firstRun

    override val chatTextSize: Flow<ChatTextSize> = size

    override val chatSidesSwapped: Flow<Boolean> = swapped

    override val themeMode: Flow<ThemeMode> = mode

    val saved: ChatProfile get() = profile.value

    override suspend fun setChatProfile(profile: ChatProfile) {
        this.profile.value = profile
    }

    override suspend fun setFirstRunDone() {
        firstRun.value = true
    }

    override suspend fun setShowScheme(scheme: ShowScheme) {
        this.scheme.value = scheme
    }

    override suspend fun setChatTextSize(size: ChatTextSize) {
        this.size.value = size
    }

    override suspend fun setChatSidesSwapped(swapped: Boolean) {
        this.swapped.value = swapped
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}
