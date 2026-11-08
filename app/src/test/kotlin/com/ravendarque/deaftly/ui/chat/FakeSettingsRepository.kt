package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initial: ChatProfile = ChatProfile(),
    showScheme: ShowScheme? = null,
    layout: ChatLayout = ChatLayout.Sides,
) : SettingsRepository {
    private val profile = MutableStateFlow(initial)
    private val scheme = MutableStateFlow(showScheme)
    private val layoutFlow = MutableStateFlow(layout)

    override val chatProfile: Flow<ChatProfile> = profile

    override val showScheme: Flow<ShowScheme?> = scheme

    override val chatLayout: Flow<ChatLayout> = layoutFlow

    val savedLayout: ChatLayout get() = layoutFlow.value

    val saved: ChatProfile get() = profile.value

    override suspend fun setChatProfile(profile: ChatProfile) {
        this.profile.value = profile
    }

    override suspend fun setChatLayout(layout: ChatLayout) {
        layoutFlow.value = layout
    }

    override suspend fun setShowScheme(scheme: ShowScheme) {
        this.scheme.value = scheme
    }
}
