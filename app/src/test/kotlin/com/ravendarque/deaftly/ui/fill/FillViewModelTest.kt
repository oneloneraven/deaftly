package com.ravendarque.deaftly.ui.fill

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.ui.home.FakeTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FillViewModelTest {
    private val repository = FakeTemplateRepository(fillTemplates)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        id: String = "check-in",
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = FillViewModel(id, repository, savedState)

    private fun FillViewModel.ready() = assertIs<FillUiState.Ready>(state.value)

    private fun TestScope.collect(viewModel: FillViewModel) =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

    @Test
    fun startsLoadingThenExposesTheRenderedTemplateWithNothingActive() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            val state = viewModel.ready()
            assertEquals("Check in", state.title)
            assertNull(state.active)
            assertFalse(state.canShow)
            assertEquals(
                listOf(
                    FillPart.Text("Hi! I'm deaf. I'd like to check in. My name is "),
                    FillPart.Empty(checkIn.placeholder("name")),
                    FillPart.Text(". Please "),
                    FillPart.Empty(checkIn.placeholder("how to reply")),
                    FillPart.Text("."),
                ),
                state.parts,
            )
        }

    @Test
    fun anUnknownTemplateIsMissing() =
        runTest {
            val viewModel = viewModel("nope")
            collect(viewModel)

            assertEquals(FillUiState.Missing, viewModel.state.value)
        }

    @Test
    fun activatingAPlaceholderExposesItWithItsCurrentValue() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.activate("how to reply")

            val active = assertIs<ActivePlaceholder>(viewModel.ready().active)
            assertEquals("how to reply", active.placeholder.key)
            assertNull(active.value)
        }

    @Test
    fun activatingAnUnknownKeyIsIgnored() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.activate("not in the body")

            assertNull(viewModel.ready().active)
        }

    @Test
    fun choosingAValueFillsTheChipAndAdvancesToTheNextUnfilledPlaceholder() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")

            viewModel.choose("Sam")

            val state = viewModel.ready()
            assertEquals("Sam", (state.parts[1] as FillPart.Filled).text)
            assertEquals("how to reply", state.active?.placeholder?.key)
            assertFalse(state.canShow)
        }

    @Test
    fun fillingTheLastPlaceholderClosesTheSheetAndEnablesShow() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")
            viewModel.choose("Sam")

            viewModel.choose("write it down")

            val state = viewModel.ready()
            assertNull(state.active)
            assertTrue(state.canShow)
            assertEquals("Hi! I'm deaf. I'd like to check in. My name is Sam. Please write it down.", state.text)
        }

    @Test
    fun changingAFilledPlaceholderJustClosesTheSheet() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")
            viewModel.choose("Sam")
            viewModel.dismiss()

            viewModel.activate("name")
            assertEquals("Sam", viewModel.ready().active?.value)
            viewModel.choose("Alex")

            val state = viewModel.ready()
            assertNull(state.active)
            assertEquals("Alex", (state.parts[1] as FillPart.Filled).text)
            assertFalse(state.canShow)
        }

    @Test
    fun blankValuesAreIgnoredAndValuesAreTrimmed() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")

            viewModel.choose("   ")
            assertEquals(
                "name",
                viewModel
                    .ready()
                    .active
                    ?.placeholder
                    ?.key,
            )
            assertTrue(viewModel.ready().parts[1] is FillPart.Empty)

            viewModel.choose("  Sam  ")
            assertEquals("Sam", (viewModel.ready().parts[1] as FillPart.Filled).text)
        }

    @Test
    fun dismissingClosesTheSheetWithoutFilling() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")

            viewModel.dismiss()

            assertNull(viewModel.ready().active)
            assertTrue(viewModel.ready().parts[1] is FillPart.Empty)
        }

    @Test
    fun choosingWithNothingActiveDoesNothing() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)

            viewModel.choose("Sam")

            assertTrue(viewModel.ready().parts[1] is FillPart.Empty)
        }

    @Test
    fun theFillAndTheActivePlaceholderSurviveRecreationFromSavedState() =
        runTest {
            val savedState = SavedStateHandle()
            val first = viewModel(savedState = savedState)
            collect(first)
            first.activate("name")
            first.choose("Sam")

            val second = viewModel(savedState = savedState)
            collect(second)

            val state = second.ready()
            assertEquals("Sam", (state.parts[1] as FillPart.Filled).text)
            assertEquals("how to reply", state.active?.placeholder?.key)
        }
}
