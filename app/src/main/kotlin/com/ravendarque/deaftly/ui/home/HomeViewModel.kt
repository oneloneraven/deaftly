package com.ravendarque.deaftly.ui.home

import androidx.lifecycle.ViewModel
import com.ravendarque.deaftly.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val destinations: List<Route>,
)

class HomeViewModel(
    destinations: List<Route>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState(destinations))
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()
}
