package com.oneloneraven.deaftly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneloneraven.deaftly.core.chat.ChatProfile
import com.oneloneraven.deaftly.core.chat.ColourRole
import com.oneloneraven.deaftly.core.settings.PersonalNames
import com.oneloneraven.deaftly.core.settings.SettingsRepository
import com.oneloneraven.deaftly.core.settings.ShowScheme
import com.oneloneraven.deaftly.core.settings.ThemeMode
import com.oneloneraven.deaftly.core.template.TemplateRepository
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
        val themeMode: ThemeMode,
        val hiddenBuiltIns: Int,
        val editedBuiltIns: Int,
        val names: PersonalNames,
    ) : SettingsUiState
}

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val templates: TemplateRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> =
        combine(
            combine(settings.chatProfile, settings.showScheme, settings.themeMode, ::Triple),
            templates.hiddenBuiltInCount,
            templates.editedBuiltInCount,
            settings.personalNames,
        ) { (profile, scheme, mode), hidden, edited, names ->
            SettingsUiState.Ready(profile, scheme, mode, hidden, edited, names)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState.Loading)

    fun setName(name: String) {
        viewModelScope.launch { settings.setChatProfile(settings.chatProfile.first().copy(name = name)) }
    }

    fun changeNames(change: (PersonalNames) -> PersonalNames) {
        viewModelScope.launch { settings.updatePersonalNames(change) }
    }

    fun setColour(colour: ColourRole) {
        viewModelScope.launch { settings.setChatProfile(settings.chatProfile.first().copy(colour = colour)) }
    }

    fun restoreBuiltInTemplates() {
        viewModelScope.launch { templates.restoreBuiltIns(includeEdited = false) }
    }

    fun restoreAllBuiltInTemplates() {
        viewModelScope.launch { templates.restoreBuiltIns(includeEdited = true) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setShowScheme(scheme: ShowScheme) {
        viewModelScope.launch { settings.setShowScheme(scheme) }
    }
}
