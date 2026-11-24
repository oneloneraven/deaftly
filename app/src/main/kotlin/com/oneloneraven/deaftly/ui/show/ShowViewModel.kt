package com.oneloneraven.deaftly.ui.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneloneraven.deaftly.core.settings.SettingsRepository
import com.oneloneraven.deaftly.core.settings.ShowScheme
import com.oneloneraven.deaftly.core.settings.ShowTextSize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface ShowUiState {
    data object Loading : ShowUiState

    data class Ready(
        val chosen: ShowScheme?,
        val textSize: ShowTextSize = ShowTextSize.Default,
    ) : ShowUiState
}

class ShowViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ShowUiState> =
        combine(settings.showScheme, settings.showTextSize) { scheme, size -> ShowUiState.Ready(scheme, size) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShowUiState.Loading)

    fun cycle(current: ShowScheme) {
        viewModelScope.launch { settings.setShowScheme(current.next()) }
    }

    fun stepTextSize(larger: Boolean) {
        viewModelScope.launch {
            val now = settings.showTextSize.first()
            val next = if (larger) now.larger() else now.smaller()
            if (next != now) settings.setShowTextSize(next)
        }
    }
}
