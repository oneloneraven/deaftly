package com.ravendarque.deaftly.ui.settings

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.core.settings.ThemeMode
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

    private val builtInA = Template("a", "A", "Everyday", "Hello", builtIn = true)
    private val builtInB = Template("b", "B", "Everyday", "Bye", builtIn = true)

    private fun TestScope.viewModel(
        settings: FakeSettingsRepository,
        templates: FakeTemplateRepository = FakeTemplateRepository(listOf(builtInA, builtInB), listOf(builtInA, builtInB)),
    ): SettingsViewModel =
        SettingsViewModel(settings, templates).also {
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
                SettingsUiState.Ready(
                    ChatProfile("Alex", ColourRole.Green),
                    ShowScheme.YellowOnBlack,
                    ThemeMode.System,
                    hiddenBuiltIns = 0,
                ),
                viewModel.ready(),
            )
        }

    @Test
    fun choosingAThemeModeSavesItAndIsReported() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel = viewModel(settings)

            viewModel.setThemeMode(ThemeMode.Dark)

            assertEquals(ThemeMode.Dark, settings.themeMode.first())
            assertEquals(ThemeMode.Dark, viewModel.ready().themeMode)
        }

    @Test
    fun reportsHowManyBuiltInTemplatesAreHidden() =
        runTest {
            val templates = FakeTemplateRepository(listOf(builtInA), listOf(builtInA, builtInB))
            val viewModel = viewModel(FakeSettingsRepository(), templates)

            assertEquals(1, viewModel.ready().hiddenBuiltIns)
        }

    @Test
    fun restoringBringsBackOnlyTheHiddenBuiltInsAndLeavesEditsAlone() =
        runTest {
            val edited = builtInA.copy(title = "Mine")
            val templates = FakeTemplateRepository(listOf(edited), listOf(builtInA, builtInB))
            val viewModel = viewModel(FakeSettingsRepository(), templates)

            viewModel.restoreBuiltInTemplates()

            assertEquals(listOf(edited, builtInB), templates.snapshot)
            assertEquals(0, viewModel.ready().hiddenBuiltIns)
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
