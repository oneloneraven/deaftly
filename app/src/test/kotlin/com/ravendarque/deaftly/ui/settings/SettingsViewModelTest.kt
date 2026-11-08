package com.ravendarque.deaftly.ui.settings

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
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
                viewModel(FakeSettingsRepository(ChatProfile("Alex", ColourRole.Teal), ShowScheme.YellowOnBlack, ChatLayout.Split))

            assertEquals(
                SettingsUiState.Ready(ChatLayout.Split, ChatProfile("Alex", ColourRole.Teal), ShowScheme.YellowOnBlack),
                viewModel.ready(),
            )
        }

    @Test
    fun choosingALayoutStoresIt() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)

            viewModel.setLayout(ChatLayout.Split)

            assertEquals(ChatLayout.Split, settings.savedLayout)
            assertEquals(ChatLayout.Split, viewModel.ready().layout)
        }

    @Test
    fun changingTheNameKeepsTheColourAndViceVersa() =
        runTest {
            val settings = FakeSettingsRepository(ChatProfile("Alex", ColourRole.Teal))
            val viewModel = viewModel(settings)

            viewModel.setName("Sam")
            assertEquals(ChatProfile("Sam", ColourRole.Teal), settings.saved)
            viewModel.setColour(ColourRole.Rose)
            assertEquals(ChatProfile("Sam", ColourRole.Rose), settings.saved)
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
