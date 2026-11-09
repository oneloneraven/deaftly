package com.ravendarque.deaftly.ui.facetoface

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaceToFaceViewModelTest {
    @Test
    fun startsEmpty() {
        assertEquals(FaceToFaceState(), FaceToFaceViewModel(SavedStateHandle()).state.value)
    }

    @Test
    fun sendingAppendsTheTrimmedMessageAndClearsTheDraft() {
        val viewModel = FaceToFaceViewModel(SavedStateHandle())
        viewModel.updateDraft("  Hello, I am deaf ")

        viewModel.send("  Hello, I am deaf ")

        assertEquals(FaceToFaceState(listOf("Hello, I am deaf"), ""), viewModel.state.value)
    }

    @Test
    fun messagesStayInTheOrderTheyWereSent() {
        val viewModel = FaceToFaceViewModel(SavedStateHandle())

        viewModel.send("One")
        viewModel.send("Two")
        viewModel.send("Three")

        assertEquals(listOf("One", "Two", "Three"), viewModel.state.value.messages)
    }

    @Test
    fun aBlankMessageIsNotSent() {
        val viewModel = FaceToFaceViewModel(SavedStateHandle())
        viewModel.updateDraft("   ")

        viewModel.send("   ")

        assertEquals(FaceToFaceState(draft = "   "), viewModel.state.value)
    }

    @Test
    fun theDraftTracksEveryKeystroke() {
        val viewModel = FaceToFaceViewModel(SavedStateHandle())

        viewModel.updateDraft("H")
        viewModel.updateDraft("He")

        assertEquals("He", viewModel.state.value.draft)
    }

    @Test
    fun messagesAndTheDraftSurviveRecreation() {
        val saved = SavedStateHandle()
        val first = FaceToFaceViewModel(saved)
        first.send("Hello")
        first.send("Could I have a coffee")
        first.updateDraft("And a")

        val restored = FaceToFaceViewModel(saved)

        assertEquals(FaceToFaceState(listOf("Hello", "Could I have a coffee"), "And a"), restored.state.value)
    }

    @Test
    fun continuingCarriesTheMessagesInOrderAndTheUnsentText() {
        val viewModel = FaceToFaceViewModel(SavedStateHandle())
        viewModel.send("Hello")
        viewModel.send("Could I have a coffee")

        assertEquals(Route.Chat(listOf("Hello", "Could I have a coffee"), "And a"), viewModel.continueAsChat(" And a "))
    }

    @Test
    fun continuingWithNothingTypedCarriesNoDraft() {
        val viewModel = FaceToFaceViewModel(SavedStateHandle())
        viewModel.send("Hello")

        assertTrue(viewModel.continueAsChat("").draft.isEmpty())
    }
}
