package com.oneloneraven.deaftly.ui.savedchats

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oneloneraven.deaftly.core.chat.Conversation
import com.oneloneraven.deaftly.core.chat.Participant
import com.oneloneraven.deaftly.core.chat.ParticipantId
import com.oneloneraven.deaftly.core.chat.RichText
import com.oneloneraven.deaftly.ui.chat.FakeSavedChatStore
import com.oneloneraven.deaftly.ui.chat.testWriteScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

            val viewModel = SavedChatsViewModel(store, testWriteScope)

            assertEquals(listOf(second, first), viewModel.chats.value.map { it.id })
        }

    @Test
    fun deleteRemovesTheChat() =
        runTest {
            val id = store.newId()
            store.save(id, chat("Kwame"))
            val viewModel = SavedChatsViewModel(store, testWriteScope)

            viewModel.delete(id)

            assertEquals(emptyList(), viewModel.chats.value)
            assertEquals(null, store.load(id))
        }

    @Test
    fun deleteFinishesEvenIfTheScreenClosesStraightAway() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val id = store.newId()
            store.save(id, chat("Kwame"))
            val owner = ViewModelStore()
            val factory =
                viewModelFactory { initializer { SavedChatsViewModel(store, CoroutineScope(StandardTestDispatcher(testScheduler))) } }
            val viewModel = ViewModelProvider.create(owner, factory)[SavedChatsViewModel::class]

            viewModel.delete(id)
            owner.clear()
            advanceUntilIdle()

            assertEquals(null, store.load(id))
            assertEquals(emptyList(), store.summaries.value)
        }

    @Test
    fun openingAChatThatStillExistsOpensIt() =
        runTest {
            val id = store.newId()
            store.save(id, chat("Kwame"))
            val viewModel = SavedChatsViewModel(store, testWriteScope)
            val opened = mutableListOf<String>()

            viewModel.open(id) { opened += it }

            assertEquals(listOf(id), opened)
            assertFalse(viewModel.couldNotOpen.value)
        }

    @Test
    fun openingAChatWhoseFileHasGoneDropsItAndSaysSo() =
        runTest {
            val id = store.newId()
            store.save(id, chat("Kwame"))
            store.loseFile(id)
            val viewModel = SavedChatsViewModel(store, testWriteScope)
            val opened = mutableListOf<String>()

            viewModel.open(id) { opened += it }

            assertEquals(emptyList(), opened)
            assertEquals(emptyList(), viewModel.chats.value)
            assertTrue(viewModel.couldNotOpen.value)
        }
}
