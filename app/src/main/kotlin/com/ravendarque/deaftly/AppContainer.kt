package com.ravendarque.deaftly

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.home.HomeViewModel

class AppContainer {
    val homeViewModelFactory =
        viewModelFactory {
            initializer { HomeViewModel(Route.entries.filter { it != Route.Home }) }
        }
}
