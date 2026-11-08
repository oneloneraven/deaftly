package com.ravendarque.deaftly.ui.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface ShowUiState {
    data object Loading : ShowUiState

    data class Ready(
        val chosen: ShowScheme?,
    ) : ShowUiState
}

class ShowViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ShowUiState> =
        settings.showScheme
            .map<ShowScheme?, ShowUiState> { ShowUiState.Ready(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShowUiState.Loading)

    fun cycle(current: ShowScheme) {
        viewModelScope.launch { settings.setShowScheme(current.next()) }
    }
}
