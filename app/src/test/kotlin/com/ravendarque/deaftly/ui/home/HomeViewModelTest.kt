package com.ravendarque.deaftly.ui.home

import com.ravendarque.deaftly.navigation.Route
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelTest {
    @Test
    fun exposesTheGivenDestinationsAsState() =
        runTest {
            val destinations = listOf(Route.Fill, Route.Chat)

            val state = HomeViewModel(destinations).state.first()

            assertEquals(HomeUiState(destinations), state)
        }
}
