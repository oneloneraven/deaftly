package com.ravendarque.deaftly.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val VALID_ID = "0f8fad5b-d9cb-469f-a165-70867728950e"
private const val OTHER_VALID_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7"

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SavedChatStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000L

    private val dir get() = File(folder.root, "chats")

    private fun TestScope.store(): FileSavedChatStore {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return FileSavedChatStore(dir, dispatcher, backgroundScope, clock = { now })
    }

    private fun conversation(
        other: String? = "Kwame",
        lines: List<String> = listOf("Hello", "See you at 6"),
    ): Conversation {
        val base = Conversation(Participant(ParticipantId.A, ColourRole.Red, "Amira"), Participant(ParticipantId.B, ColourRole.Blue, other))
        return lines.foldIndexed(base) { index, chat, line ->
            val styles = if (index == 0) setOf(SpanStyle.Bold) else emptySet()
            val quote = if (index == 1) chat.quote(0) else null
            chat.append(listOf(Span(line, styles)), quote).switchTurn()
        }
    }

    @Test
    fun savingThenLoadingRestoresTheConversationExactly() =
        runTest {
            val store = store()
            val chat = conversation().withTurn(ParticipantId.B)

            store.save(VALID_ID, chat)
            val loaded = store.load(VALID_ID)

            assertEquals(chat, loaded?.conversation)
            assertEquals(now, loaded?.changedAt)
        }

    @Test
    fun summariesListNewestFirstWithOtherNameAndLastLine() =
        runTest {
            val store = store()
            now = 1
            store.save(VALID_ID, conversation(other = "Kwame"))
            now = 2
            store.save(OTHER_VALID_ID, conversation(other = null, lines = emptyList()))

            val summaries = store.summaries.value

            assertEquals(listOf(OTHER_VALID_ID, VALID_ID), summaries.map { it.id })
            assertEquals(SavedChatSummary(VALID_ID, "Kwame", "See you at 6", 1), summaries[1])
            assertEquals(SavedChatSummary(OTHER_VALID_ID, null, null, 2), summaries[0])
        }

    @Test
    fun summariesAreReadFromFilesAlreadyThere() =
        runTest {
            store().save(VALID_ID, conversation())

            val reopened = store()

            assertEquals(listOf(VALID_ID), reopened.summaries.value.map { it.id })
        }

    @Test
    fun deleteRemovesTheFileAndTheSummary() =
        runTest {
            val store = store()
            store.save(VALID_ID, conversation())

            store.delete(VALID_ID)

            assertFalse(File(dir, "$VALID_ID.json").exists())
            assertNull(store.load(VALID_ID))
            assertTrue(store.summaries.value.none { it.id == VALID_ID })
        }

    @Test
    fun aCorruptFileIsSkipped() =
        runTest {
            dir.mkdirs()
            File(dir, "$VALID_ID.json").writeText("{\"version\":1,")
            File(dir, "$OTHER_VALID_ID.json").writeText("")

            val store = store()

            assertEquals(emptyList(), store.summaries.value)
            assertNull(store.load(VALID_ID))
            assertNull(store.load(OTHER_VALID_ID))
        }

    @Test
    fun refusesIdsThatAreNotItsOwn() =
        runTest {
            val store = store()

            assertFailsWith<IllegalArgumentException> { store.save("../templates", conversation()) }
            assertNull(store.load("../templates"))
            assertFalse(File(folder.root, "templates.json").exists())
            assertEquals(
                emptyList(),
                folder.root
                    .walk()
                    .filter { it.isFile }
                    .toList(),
            )
        }

    @Test
    fun newIdsAreDistinctAndAccepted() =
        runTest {
            val store = store()
            val first = store.newId()
            val second = store.newId()

            store.save(first, conversation())
            store.save(second, conversation())

            assertNotEquals(first, second)
            assertEquals(
                setOf(first, second),
                store.summaries.value
                    .map { it.id }
                    .toSet(),
            )
        }
}
