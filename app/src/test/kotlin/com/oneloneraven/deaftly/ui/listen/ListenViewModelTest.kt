package com.oneloneraven.deaftly.ui.listen

import androidx.lifecycle.SavedStateHandle
import com.oneloneraven.deaftly.core.speech.FakeSpeechRecogniser
import com.oneloneraven.deaftly.core.speech.SpeechEvent
import com.oneloneraven.deaftly.navigation.Route
import com.oneloneraven.deaftly.ui.chat.FakeSettingsRepository
import com.oneloneraven.deaftly.ui.chat.HangingAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListenViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val settings = FakeSettingsRepository()

    private fun TestScope.listening(
        speech: List<SpeechEvent?>,
        audio: HangingAudio = HangingAudio(speech.size),
        savedState: SavedStateHandle = SavedStateHandle(),
    ): ListenViewModel {
        val viewModel =
            ListenViewModel(settings, savedState, { FakeSpeechRecogniser(speech) }, audio, StandardTestDispatcher(testScheduler))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.setListeningAllowed(true)
        runCurrent()
        return viewModel
    }

    @Test
    fun listensAsSoonAsItOpens() =
        runTest {
            val audio = HangingAudio(0)
            val viewModel = listening(emptyList(), audio)

            assertTrue(viewModel.state.value.voice.on)
            assertTrue(audio.isOpen)
        }

    @Test
    fun keepsEachUtteranceInOrder() =
        runTest {
            val viewModel = listening(listOf(SpeechEvent.Utterance("Hello there"), SpeechEvent.Utterance(" Table for two? ")))

            assertEquals(listOf("Hello there", "Table for two?"), viewModel.state.value.heard)
            assertFalse(viewModel.isEmpty)
        }

    @Test
    fun pausingStopsListeningAndResumingStartsAgain() =
        runTest {
            val audio = HangingAudio(0)
            val viewModel = listening(emptyList(), audio)

            viewModel.toggleListening()
            runCurrent()
            assertFalse(viewModel.state.value.voice.on)
            assertFalse(audio.isOpen)

            viewModel.toggleListening()
            runCurrent()
            assertTrue(viewModel.state.value.voice.on)
            assertTrue(audio.isOpen)
        }

    @Test
    fun aPauseSurvivesRecreation() =
        runTest {
            val savedState = SavedStateHandle()
            listening(emptyList(), HangingAudio(0), savedState).toggleListening()

            val recreated = listening(emptyList(), HangingAudio(0), savedState)

            assertFalse(recreated.state.value.voice.on)
        }

    @Test
    fun leavingTheScreenStopsTheMicrophone() =
        runTest {
            val audio = HangingAudio(0)
            val viewModel = listening(emptyList(), audio)

            viewModel.setListeningAllowed(false)
            runCurrent()

            assertFalse(audio.isOpen)
            assertTrue(viewModel.state.value.voice.on)
        }

    @Test
    fun continuingCarriesWhatTheySaidIncludingTheLiveLine() =
        runTest {
            val audio = HangingAudio(2)
            val viewModel = listening(listOf(SpeechEvent.Utterance("Any allergies?"), SpeechEvent.Partial("We also have")), audio)

            assertEquals(Route.Chat(heard = listOf("Any allergies?", "We also have")), viewModel.continueAsChat())
            runCurrent()
            assertFalse(audio.isOpen)
        }

    @Test
    fun flippingTheSignIsRemembered() =
        runTest {
            val viewModel = listening(emptyList(), HangingAudio(0))

            viewModel.flipSign()
            runCurrent()

            assertFalse(settings.listenSignFacesThem.first())
            assertFalse(viewModel.state.value.signFacesThem)
        }
}
