package com.oneloneraven.deaftly.ui.show

import com.oneloneraven.deaftly.core.settings.ShowScheme
import com.oneloneraven.deaftly.core.settings.ShowTextSize
import com.oneloneraven.deaftly.ui.chat.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

    @Test
    fun opensAtMediumAndTheVolumeKeysStepTheSizeAndRememberIt() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = ShowViewModel(settings)
            viewModel.state.launchIn(backgroundScope)
            advanceUntilIdle()
            assertEquals(ShowTextSize.Medium, (viewModel.state.value as ShowUiState.Ready).textSize)

            viewModel.stepTextSize(larger = true)
            advanceUntilIdle()

            assertEquals(ShowTextSize.Large, (viewModel.state.value as ShowUiState.Ready).textSize)
            assertEquals(ShowTextSize.Large, settings.showTextSize.first())
        }

    @Test
    fun stepsStopAtSmallAndLarge() =
        runTest {
            val settings = FakeSettingsRepository(showTextSize = ShowTextSize.Large)
            val viewModel = ShowViewModel(settings)
            viewModel.state.launchIn(backgroundScope)

            viewModel.stepTextSize(larger = true)
            advanceUntilIdle()
            assertEquals(ShowTextSize.Large, settings.showTextSize.first())

            repeat(3) { viewModel.stepTextSize(larger = false) }
            advanceUntilIdle()
            assertEquals(ShowTextSize.Small, settings.showTextSize.first())
        }
}
