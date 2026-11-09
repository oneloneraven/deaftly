package com.ravendarque.deaftly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Ready(
        val profile: ChatProfile,
        val showScheme: ShowScheme?,
    ) : SettingsUiState
}

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> =
        combine(settings.chatProfile, settings.showScheme) { profile, scheme ->
            SettingsUiState.Ready(profile, scheme)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState.Loading)

    fun setName(name: String) {
        viewModelScope.launch { settings.setChatProfile(settings.chatProfile.first().copy(name = name)) }
    }

    fun setColour(colour: ColourRole) {
        viewModelScope.launch { settings.setChatProfile(settings.chatProfile.first().copy(colour = colour)) }
    }

    fun setShowScheme(scheme: ShowScheme) {
        viewModelScope.launch { settings.setShowScheme(scheme) }
    }
}
