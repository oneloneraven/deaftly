package com.ravendarque.deaftly.ui.chat

import androidx.lifecycle.SavedStateHandle
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.RichText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatKeeperTest {
    private val store = FakeSavedChatStore()

    private fun TestScope.keeper(savedState: SavedStateHandle = SavedStateHandle()) =
        ChatKeeper(store, savedState, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

    private fun chat(vararg lines: String): Conversation =
        lines.fold(Conversation(Participant.defaultA(), Participant.defaultB())) { chat, line -> chat.append(RichText.spans(line)) }

    @Test
    fun persistDoesNothingUntilKept() =
        runTest {
            val keeper = keeper()

            keeper.persist(chat("Hello"))
            assertEquals(0, store.saves)
            assertNull(keeper.keptId.value)

            keeper.keep(chat("Hello"))
            assertEquals(1, store.saves)
            assertNotNull(keeper.keptId.value)
        }

    @Test
    fun theIdSurvivesRecreationFromSavedState() =
        runTest {
            val savedState = SavedStateHandle()
            val first = keeper(savedState)
            first.keep(chat("Hello"))
            val id = first.keptId.value

            val restored = keeper(savedState)
            restored.persist(chat("Hello", "Again"))

            assertEquals(id, restored.keptId.value)
            assertEquals(chat("Hello", "Again"), store.load(id!!)?.conversation)
        }

    @Test
    fun quickSuccessiveSavesEndWithTheLatest() =
        runTest {
            val keeper = keeper()
            keeper.keep(chat("One"))

            keeper.persist(chat("One", "Two"))
            keeper.persist(chat("One", "Two", "Three"))

            assertEquals(chat("One", "Two", "Three"), store.load(keeper.keptId.value!!)?.conversation)
        }

    @Test
    fun openingAKeptChatMakesItKept() =
        runTest {
            val id = store.newId()
            store.save(id, chat("Hello"))
            val keeper = keeper()

            val opened = keeper.open(id)

            assertEquals(chat("Hello"), opened)
            assertEquals(id, keeper.keptId.value)
        }

    @Test
    fun openingAMissingChatLeavesItUnkept() =
        runTest {
            val keeper = keeper()

            assertNull(keeper.open(store.newId()))
            assertNull(keeper.keptId.value)
        }
}
