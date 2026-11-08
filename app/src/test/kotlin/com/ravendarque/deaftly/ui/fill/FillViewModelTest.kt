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
            assertEquals(emptyList(), active.values)
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
            assertEquals("Sam", (state.parts[1] as FillPart.Filled).values.single())
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
            assertEquals(listOf("Sam"), viewModel.ready().active?.values)
            viewModel.choose("Alex")

            val state = viewModel.ready()
            assertNull(state.active)
            assertEquals("Alex", (state.parts[1] as FillPart.Filled).values.single())
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
            assertEquals("Sam", (viewModel.ready().parts[1] as FillPart.Filled).values.single())
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
            assertEquals("Sam", (state.parts[1] as FillPart.Filled).values.single())
            assertEquals("how to reply", state.active?.placeholder?.key)
        }

    private fun FillViewModel.menuItems() = (ready().parts.filterIsInstance<FillPart.Filled>().single()).values

    @Test
    fun aRepeatablePlaceholderCollectsEachChosenValueAndTheTextJoinsThem() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)

            viewModel.activate("menu item")
            viewModel.choose("Pasta arrabbiata")
            assertNull(viewModel.ready().active)
            viewModel.activate("menu item")
            viewModel.choose("Garlic bread")
            viewModel.activate("menu item")
            viewModel.choose("Tiramisu")

            assertEquals(listOf("Pasta arrabbiata", "Garlic bread", "Tiramisu"), viewModel.menuItems())
            assertNull(viewModel.ready().active)
            assertTrue(viewModel.ready().canShow)
            assertEquals(
                "Hi! I'm deaf. Could I have Pasta arrabbiata, Garlic bread and Tiramisu please? " +
                    "If you have a question, please type it on my phone.",
                viewModel.ready().text,
            )
        }

    @Test
    fun addingAValueTheRepeatablePlaceholderAlreadyHasChangesNothing() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)
            viewModel.activate("menu item")
            viewModel.choose("Tiramisu")

            viewModel.activate("menu item")
            viewModel.choose("Tiramisu")

            assertEquals(listOf("Tiramisu"), viewModel.menuItems())
        }

    @Test
    fun removingValuesLeavesTheRestAndEmptyingThePlaceholderUnfillsIt() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)
            listOf("A", "B").forEach {
                viewModel.activate("menu item")
                viewModel.choose(it)
            }

            viewModel.remove("menu item", 0)
            assertEquals(listOf("B"), viewModel.menuItems())
            assertTrue(viewModel.ready().canShow)

            viewModel.remove("menu item", 0)
            assertTrue(viewModel.ready().parts.any { it is FillPart.Empty })
            assertFalse(viewModel.ready().canShow)
        }

    @Test
    fun removingAMissingIndexIsIgnored() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)
            viewModel.activate("menu item")
            viewModel.choose("A")

            viewModel.remove("menu item", 3)

            assertEquals(listOf("A"), viewModel.menuItems())
        }

    @Test
    fun repeatableValuesSurviveRecreationFromSavedState() =
        runTest {
            val savedState = SavedStateHandle()
            val first = viewModel("order-food", savedState)
            collect(first)
            listOf("A", "B").forEach {
                first.activate("menu item")
                first.choose(it)
            }

            val second = viewModel("order-food", savedState)
            collect(second)

            assertEquals(listOf("A", "B"), second.menuItems())
        }
}
