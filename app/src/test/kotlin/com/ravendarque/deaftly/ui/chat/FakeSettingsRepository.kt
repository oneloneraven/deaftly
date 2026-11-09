package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initial: ChatProfile = ChatProfile(),
    showScheme: ShowScheme? = null,
    firstRunDone: Boolean = true,
) : SettingsRepository {
    private val profile = MutableStateFlow(initial)
    private val scheme = MutableStateFlow(showScheme)
    private val firstRun = MutableStateFlow(firstRunDone)

    override val chatProfile: Flow<ChatProfile> = profile

    override val showScheme: Flow<ShowScheme?> = scheme

    override val firstRunDone: Flow<Boolean> = firstRun

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
}
