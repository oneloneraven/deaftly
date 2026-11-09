package com.ravendarque.deaftly.ui.firstrun

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import com.ravendarque.deaftly.ui.chat.PARTICIPANT_NAME_MAX_LENGTH
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FirstRunViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun pending() = FakeSettingsRepository(firstRunDone = false)

    @Test
    fun startsWithNoNameAndTheAccentColour() {
        assertEquals(FirstRunState("", ColourRole.Indigo), FirstRunViewModel(pending()).state.value)
    }

    @Test
    fun choicesAreKeptInTheDraftAndNothingIsSavedYet() {
        val settings = pending()
        val viewModel = FirstRunViewModel(settings)

        viewModel.setName("Sam")
        viewModel.setColour(ColourRole.Green)

        assertEquals(FirstRunState("Sam", ColourRole.Green), viewModel.state.value)
        assertEquals(ChatProfile(), settings.saved)
    }

    @Test
    fun theNameIsLimitedToTheParticipantNameLength() {
        val viewModel = FirstRunViewModel(pending())

        viewModel.setName("x".repeat(PARTICIPANT_NAME_MAX_LENGTH + 10))

        assertEquals(PARTICIPANT_NAME_MAX_LENGTH, viewModel.state.value.name.length)
    }

    @Test
    fun gettingStartedSavesTheChoicesMarksFirstRunDoneAndThenLeaves() =
        runTest {
            val settings = pending()
            val viewModel = FirstRunViewModel(settings)
            viewModel.setName("Sam")
            viewModel.setColour(ColourRole.Red)
            var left = false

            viewModel.getStarted { left = true }

            assertEquals(ChatProfile("Sam", ColourRole.Red), settings.saved)
            assertTrue(settings.firstRunDone.first())
            assertTrue(left)
        }

    @Test
    fun theNameIsOptional() =
        runTest {
            val settings = pending()

            FirstRunViewModel(settings).getStarted {}

            assertTrue(settings.firstRunDone.first())
            assertEquals(ChatProfile(null, ColourRole.Indigo), settings.saved)
        }

    @Test
    fun firstRunIsNotDoneUntilGettingStarted() =
        runTest {
            val settings = pending()
            FirstRunViewModel(settings).setName("Sam")

            assertFalse(settings.firstRunDone.first())
        }
}
