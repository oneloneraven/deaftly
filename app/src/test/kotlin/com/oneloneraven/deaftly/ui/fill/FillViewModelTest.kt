package com.oneloneraven.deaftly.ui.fill

import androidx.lifecycle.SavedStateHandle
import com.oneloneraven.deaftly.core.settings.PersonalNames
import com.oneloneraven.deaftly.core.template.PersonalField
import com.oneloneraven.deaftly.ui.chat.FakeSettingsRepository
import com.oneloneraven.deaftly.ui.home.FakeTemplateRepository
import com.oneloneraven.deaftly.ui.ocr.OcrOutcome
import com.oneloneraven.deaftly.ui.ocr.OcrOutcomes
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

    private val outcomes = OcrOutcomes()

    private val settings = FakeSettingsRepository()

    private fun viewModel(
        id: String = "check-in",
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = FillViewModel(id, repository, settings, savedState, outcomes)

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
    fun choosingAValueFillsTheChipAndClosesTheSheetWithoutOpeningTheNextGap() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")

            viewModel.choose("Amara")

            val state = viewModel.ready()
            assertEquals("Amara", (state.parts[1] as FillPart.Filled).values.single())
            assertNull(state.active)
            assertFalse(state.canShow)
        }

    @Test
    fun fillingTheLastPlaceholderClosesTheSheetAndEnablesShow() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")
            viewModel.choose("Sam")
            viewModel.activate("how to reply")

            viewModel.choose("write it down")

            val state = viewModel.ready()
            assertNull(state.active)
            assertTrue(state.canShow)
            assertEquals("Hi! I'm deaf. I'd like to check in. My name is Sam. Please write it down.", state.text)
        }

    @Test
    fun exposesTheRangesOfTheFilledValuesInTheRenderedText() =
        runTest {
            val viewModel = viewModel()
            collect(viewModel)
            viewModel.activate("name")
            viewModel.choose("Sam")
            viewModel.activate("how to reply")
            viewModel.choose("write it down")

            val state = viewModel.ready()
            assertEquals(listOf("Sam", "write it down"), state.highlights.map { state.text.substring(it.start, it.end) })
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
            first.activate("how to reply")

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

    @Test
    fun scannedTextFillsThePlaceholderItCameFromAndIsConsumed() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)
            viewModel.activate("menu item")

            outcomes.post(OcrOutcome.Text("order-food", "menu item", "  Pasta arrabbiata "))

            val state = viewModel.ready()
            assertEquals(listOf("Pasta arrabbiata"), (state.parts[1] as FillPart.Filled).values)
            assertNull(state.active)
            assertTrue(outcomes.pending.value.isEmpty())
        }

    @Test
    fun scannedTextAppendsToARepeatablePlaceholder() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)
            viewModel.activate("menu item")
            viewModel.choose("Garlic bread")
            viewModel.activate("menu item")

            outcomes.post(OcrOutcome.Text("order-food", "menu item", "Lasagne"))

            assertEquals(listOf("Garlic bread", "Lasagne"), (viewModel.ready().parts[1] as FillPart.Filled).values)
        }

    @Test
    fun scannedTextForAnotherTemplateOrAnUnknownPlaceholderIsLeftAlone() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)

            val other = OcrOutcome.Text("check-in", "name", "Sam")
            outcomes.post(other)
            outcomes.post(OcrOutcome.Text("order-food", "starter", "Soup"))

            assertEquals(listOf(other), outcomes.pending.value)
            assertTrue((viewModel.ready().parts[1] is FillPart.Empty))
        }

    @Test
    fun typeInsteadReopensThePlaceholderWithTheTypingField() =
        runTest {
            val viewModel = viewModel("order-food")
            collect(viewModel)

            outcomes.post(OcrOutcome.TypeInstead("order-food", "menu item"))

            val active = assertIs<ActivePlaceholder>(viewModel.ready().active)
            assertEquals("menu item", active.placeholder.key)
            assertTrue(active.typeInstead)
            viewModel.dismiss()
            viewModel.activate("menu item")
            assertFalse(assertIs<ActivePlaceholder>(viewModel.ready().active).typeInstead)
        }

    @Test
    fun scannedTextThatArrivesBeforeTheTemplateLoadsIsAppliedOnceItHas() =
        runTest {
            outcomes.post(OcrOutcome.Text("order-food", "menu item", "Lasagne"))
            val viewModel = viewModel("order-food")
            collect(viewModel)

            assertEquals(listOf("Lasagne"), (viewModel.ready().parts[1] as FillPart.Filled).values)
        }

    private fun namedCheckIn(field: PersonalField) =
        checkIn.copy(
            id = "named-check-in",
            placeholders = checkIn.placeholders + ("name" to checkIn.placeholder("name").copy(fillsWith = field)),
        )

    private fun namedViewModel(
        names: PersonalNames,
        savedState: SavedStateHandle = SavedStateHandle(),
        store: FakeSettingsRepository = FakeSettingsRepository(names = names),
        field: PersonalField = PersonalField.LegalName,
    ) = FillViewModel("named-check-in", FakeTemplateRepository(fillTemplates + namedCheckIn(field)), store, savedState, outcomes)

    @Test
    fun aGapThatFillsWithASavedNameStartsWithItFromSettings() =
        runTest {
            val viewModel = namedViewModel(PersonalNames("Amira", "Hassan"))
            collect(viewModel)

            assertEquals(
                listOf(FillPart.Filled(namedCheckIn(PersonalField.LegalName).placeholder("name"), listOf("Amira Hassan"))),
                viewModel.ready().parts.filterIsInstance<FillPart.Filled>(),
            )
            assertTrue(viewModel.ready().text.startsWith("Hi! I'm deaf. I'd like to check in. My name is Amira Hassan."))
        }

    @Test
    fun eachSavedNameFillsInItsOwnValue() =
        runTest {
            val names = PersonalNames("Tunde", "Bello", legalNameDiffers = true, legalName = "Olatunde Bello")
            mapOf(
                PersonalField.FirstName to "Tunde",
                PersonalField.LastName to "Bello",
                PersonalField.FullName to "Tunde Bello",
                PersonalField.LegalName to "Olatunde Bello",
            ).forEach { (field, expected) ->
                val viewModel = namedViewModel(names, field = field)
                collect(viewModel)

                assertTrue("My name is $expected." in viewModel.ready().text, field.name)
            }
        }

    @Test
    fun theNameGapStaysEmptyWhenNoNamesAreSet() =
        runTest {
            val viewModel = namedViewModel(PersonalNames())
            collect(viewModel)

            assertTrue(viewModel.ready().parts.any { it is FillPart.Empty && it.placeholder.key == "name" })
        }

    @Test
    fun aGapWithoutAPersonalFieldIsNeverPreFilled() =
        runTest {
            val viewModel =
                FillViewModel(
                    "check-in",
                    repository,
                    FakeSettingsRepository(names = PersonalNames("Amira", "Hassan")),
                    SavedStateHandle(),
                    outcomes,
                )
            collect(viewModel)

            assertTrue(viewModel.ready().parts.any { it is FillPart.Empty && it.placeholder.key == "name" })
        }

    @Test
    fun aValueAlreadyThereIsNotOverwritten() =
        runTest {
            val saved = SavedStateHandle()
            val first = namedViewModel(PersonalNames("Amira", "Hassan"), saved)
            collect(first)
            first.activate("name")
            first.choose("Mira")

            val second = namedViewModel(PersonalNames("Amira", "Hassan"), saved)
            collect(second)

            assertTrue("My name is Mira." in second.ready().text)
        }

    @Test
    fun changingThePreFilledValueAffectsOnlyThisMessage() =
        runTest {
            val store = FakeSettingsRepository(names = PersonalNames("Amira", "Hassan"))
            val viewModel = namedViewModel(PersonalNames(), store = store)
            collect(viewModel)

            viewModel.activate("name")
            viewModel.choose("Mira")

            assertTrue("My name is Mira." in viewModel.ready().text)
            assertEquals(PersonalNames("Amira", "Hassan"), store.savedNames)
        }
}
