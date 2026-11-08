package com.ravendarque.deaftly.ui.show

import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ShowScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeSettingsRepository(
    initial: ShowScheme? = null,
) : SettingsRepository {
    private val current = MutableStateFlow(initial)

    override val showScheme: Flow<ShowScheme?> = current

    override suspend fun setShowScheme(scheme: ShowScheme) {
        current.value = scheme
    }
}

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
            val viewModel = ShowViewModel(FakeSettingsRepository(ShowScheme.WhiteOnBlack))
            viewModel.state.launchIn(backgroundScope)
            advanceUntilIdle()

            viewModel.cycle(ShowScheme.WhiteOnBlack)
            advanceUntilIdle()

            assertEquals(ShowUiState.Ready(ShowScheme.YellowOnBlack), viewModel.state.value)
        }
}
