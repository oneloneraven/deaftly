package com.ravendarque.deaftly.ui.savedchats

import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.RichText
import com.ravendarque.deaftly.ui.chat.FakeSavedChatStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SavedChatsViewModelTest {
    private val store = FakeSavedChatStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun chat(other: String) =
        Conversation(Participant.defaultA(), Participant.defaultB(other)).append(RichText.spans("Hello")).withTurn(ParticipantId.B)

    @Test
    fun listsWhatTheStoreHolds() =
        runTest {
            val first = store.newId()
            val second = store.newId()
            store.save(first, chat("Kwame"))
            store.save(second, chat("Siobhan"))

            val viewModel = SavedChatsViewModel(store)

            assertEquals(listOf(second, first), viewModel.chats.value.map { it.id })
        }

    @Test
    fun deleteRemovesTheChat() =
        runTest {
            val id = store.newId()
            store.save(id, chat("Kwame"))
            val viewModel = SavedChatsViewModel(store)

            viewModel.delete(id)

            assertEquals(emptyList(), viewModel.chats.value)
            assertEquals(null, store.load(id))
        }
}
