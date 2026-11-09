package com.ravendarque.deaftly.ui.settings

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(settings: FakeSettingsRepository): SettingsViewModel =
        SettingsViewModel(settings).also {
            it.state.launchIn(backgroundScope)
            advanceUntilIdle()
        }

    private fun SettingsViewModel.ready() = assertIs<SettingsUiState.Ready>(state.value)

    @Test
    fun reportsTheStoredSettings() =
        runTest {
            val viewModel =
                viewModel(FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green), ShowScheme.YellowOnBlack))

            assertEquals(
                SettingsUiState.Ready(ChatProfile("Alex", ColourRole.Green), ShowScheme.YellowOnBlack),
                viewModel.ready(),
            )
        }

    @Test
    fun changingTheNameKeepsTheColourAndViceVersa() =
        runTest {
            val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Green))
            val viewModel = viewModel(settings)

            viewModel.setName("Sam")
            assertEquals(ChatProfile("Sam", ColourRole.Green), settings.saved)
            viewModel.setColour(ColourRole.Red)
            assertEquals(ChatProfile("Sam", ColourRole.Red), settings.saved)
        }

    @Test
    fun choosingAShowSchemeStoresIt() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)

            viewModel.setShowScheme(ShowScheme.WhiteOnBlack)

            assertEquals(ShowScheme.WhiteOnBlack, viewModel.ready().showScheme)
        }
}
