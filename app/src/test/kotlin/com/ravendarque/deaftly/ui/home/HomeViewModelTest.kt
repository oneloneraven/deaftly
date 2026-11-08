package com.ravendarque.deaftly.ui.home

import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.RenderedPart
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val checkIn =
        Template(
            id = "check-in",
            title = "Check in",
            category = "Everyday",
            body = "Hi! I'm deaf. My name is {name}.",
            pinned = true,
            placeholders = mapOf("name" to Placeholder("name", "Your name", PlaceholderType.Typing)),
        )
    private val order = Template("order", "Order food", "Food & drink", "Could I have {item} please?", pinned = true)
    private val cafe = Template("cafe", "Café", "Food & drink", "A flat white, please.")
    private val doctor = Template("doctor", "At the doctor's", "Health", "Please write down what the doctor says.")
    private val all = listOf(checkIn, order, cafe, doctor)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(repository: TemplateRepository = FakeTemplateRepository(all)) =
        HomeViewModel(repository).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        }

    private fun TestScope.viewModel(templates: List<Template>) = viewModel(FakeTemplateRepository(templates))

    @Test
    fun startsLoadingThenExposesTheRepositoryTemplates() =
        runTest {
            val state = viewModel().state.value
            assertFalse(state.isLoading)

            assertEquals(listOf("check-in", "order", "cafe", "doctor"), state.templates.map { it.id })
            assertEquals(listOf("check-in", "order"), state.pinned.map { it.id })
            assertEquals("", state.query)
            assertNull(state.selectedCategory)
        }

    @Test
    fun isLoadingUntilTheRepositoryEmits() =
        runTest {
            val silent =
                object : TemplateRepository by FakeTemplateRepository(emptyList()) {
                    override val templates: Flow<List<Template>> = flow { awaitCancellation() }
                }

            assertTrue(viewModel(silent).state.value.isLoading)
        }

    @Test
    fun listsCategoriesInOrderOfFirstAppearanceWithoutDuplicates() =
        runTest {
            assertEquals(listOf("Everyday", "Food & drink", "Health"), viewModel().state.value.categories)
        }

    @Test
    fun previewsShowUnfilledPlaceholdersAsLabels() =
        runTest {
            val preview =
                viewModel()
                    .state.value.templates
                    .first { it.id == "check-in" }
                    .preview

            assertEquals(
                listOf(RenderedPart.Text("Hi! I'm deaf. My name is "), RenderedPart.Unfilled("name", "Your name"), RenderedPart.Text(".")),
                preview,
            )
        }

    @Test
    fun filtersByCategoryAndKeepsPinnedTiles() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCategorySelected("Food & drink")

            val state = viewModel.state.value

            assertEquals("Food & drink", state.selectedCategory)
            assertEquals(listOf("order", "cafe"), state.templates.map { it.id })
            assertEquals(listOf("check-in", "order"), state.pinned.map { it.id })
        }

    @Test
    fun selectingAllClearsTheCategoryFilter() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCategorySelected("Health")
            viewModel.onCategorySelected(null)

            val state = viewModel.state.value

            assertNull(state.selectedCategory)
            assertEquals(4, state.templates.size)
        }

    @Test
    fun searchMatchesTitleAndBodyIgnoringCaseAndAccents() =
        runTest {
            val viewModel = viewModel()

            viewModel.onQueryChange("CAFE")
            assertEquals(
                listOf("cafe"),
                viewModel.state.value.templates
                    .map { it.id },
            )

            viewModel.onQueryChange("write DOWN")
            assertEquals(
                listOf("doctor"),
                viewModel.state.value.templates
                    .map { it.id },
            )

            viewModel.onQueryChange("Hi!")
            assertEquals(
                listOf("check-in"),
                viewModel.state.value.templates
                    .map { it.id },
            )
        }

    @Test
    fun searchAlsoFiltersThePinnedTiles() =
        runTest {
            val viewModel = viewModel()
            viewModel.onQueryChange("order")

            val state = viewModel.state.value

            assertEquals(listOf("order"), state.pinned.map { it.id })
            assertEquals("order", state.query)
        }

    @Test
    fun searchAndCategoryCombine() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCategorySelected("Food & drink")
            viewModel.onQueryChange("flat white")

            assertEquals(
                listOf("cafe"),
                viewModel.state.value.templates
                    .map { it.id },
            )
        }

    @Test
    fun noMatchGivesAnEmptyListAndClearSearchRestoresIt() =
        runTest {
            val viewModel = viewModel()
            viewModel.onQueryChange("zzz")
            val empty = viewModel.state.value
            assertTrue(empty.templates.isEmpty())
            assertTrue(empty.pinned.isEmpty())
            assertEquals("zzz", empty.query)

            viewModel.onClearSearch()

            val restored = viewModel.state.value
            assertEquals("", restored.query)
            assertEquals(4, restored.templates.size)
        }

    @Test
    fun aSelectedCategoryThatNoLongerExistsFallsBackToAll() =
        runTest {
            val repository = FakeTemplateRepository(all)
            val viewModel = viewModel(repository)
            viewModel.onCategorySelected("Health")
            assertEquals(
                listOf("doctor"),
                viewModel.state.value.templates
                    .map { it.id },
            )

            repository.delete("doctor")

            val state = viewModel.state.value
            assertNull(state.selectedCategory)
            assertFalse("Health" in state.categories)
        }

    @Test
    fun anEmptyRepositoryHasNoPinnedTilesOrCategories() =
        runTest {
            val state = viewModel(emptyList()).state.value

            assertTrue(state.templates.isEmpty())
            assertTrue(state.pinned.isEmpty())
            assertTrue(state.categories.isEmpty())
        }
}
