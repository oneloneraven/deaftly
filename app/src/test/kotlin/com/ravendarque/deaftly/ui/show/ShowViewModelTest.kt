package com.ravendarque.deaftly.ui.show

import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ShowViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun reportsNoChoiceUntilOneIsMade() =
        runTest {
            val viewModel = ShowViewModel(FakeSettingsRepository())
            viewModel.state.launchIn(backgroundScope)
            advanceUntilIdle()

            assertEquals(ShowUiState.Ready(null), viewModel.state.value)
        }

    @Test
    fun cyclingStoresTheNextScheme() =
        runTest {
            val viewModel = ShowViewModel(FakeSettingsRepository(showScheme = ShowScheme.WhiteOnBlack))
            viewModel.state.launchIn(backgroundScope)
            advanceUntilIdle()

            viewModel.cycle(ShowScheme.WhiteOnBlack)
            advanceUntilIdle()

            assertEquals(ShowUiState.Ready(ShowScheme.YellowOnBlack), viewModel.state.value)
        }
}
