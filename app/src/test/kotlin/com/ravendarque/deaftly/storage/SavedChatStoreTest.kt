package com.ravendarque.deaftly.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.core.chat.SpanStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
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
    fun aCorruptFileIsListedAsUnreadableAndCanBeDeleted() =
        runTest {
            dir.mkdirs()
            File(dir, "$VALID_ID.json").apply { writeText("{\"version\":1,") }.setLastModified(5_000)
            File(dir, "$OTHER_VALID_ID.json").writeText("")
            File(dir, "notes.json").writeText("{}")

            val store = store()

            assertEquals(
                setOf(VALID_ID, OTHER_VALID_ID),
                store.summaries.value
                    .map { it.id }
                    .toSet(),
            )
            assertTrue(store.summaries.value.all { !it.readable })
            assertEquals(
                5_000,
                store.summaries.value
                    .single { it.id == VALID_ID }
                    .changedAt,
            )
            assertNull(store.load(VALID_ID))

            store.delete(VALID_ID)

            assertFalse(File(dir, "$VALID_ID.json").exists())
            assertEquals(listOf(OTHER_VALID_ID), store.summaries.value.map { it.id })
        }

    @Test
    fun refusesIdsThatAreNotItsOwn() =
        runTest {
            val store = store()
            store.save(VALID_ID, conversation())
            val validChat = File(dir, "$VALID_ID.json").readText()
            store.delete(VALID_ID)
            val outside = File(folder.root, "templates.json").apply { writeText(validChat) }
            val further = File(folder.root.parentFile, "x.json")

            assertFailsWith<IllegalArgumentException> { store.save("../templates", conversation()) }
            assertNull(store.load("../templates"))
            store.delete("../templates")
            store.delete("../../x")

            assertEquals(validChat, outside.readText())
            assertFalse(further.exists())
            assertEquals(
                listOf(outside),
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

    @Test
    fun isLoadedOnlyOnceTheFolderHasBeenRead() =
        runTest {
            val store = FileSavedChatStore(dir, StandardTestDispatcher(testScheduler), backgroundScope, clock = { now })
            assertFalse(store.loaded.value)

            runCurrent()

            assertTrue(store.loaded.value)
        }

    @Test
    fun aChatThatCannotBeDeletedStaysListed() =
        runTest {
            val store = store()
            store.save(VALID_ID, conversation())
            dir.setWritable(false)
            try {
                assertFalse(store.delete(VALID_ID))
                assertEquals(listOf(VALID_ID), store.summaries.value.map { it.id })
            } finally {
                dir.setWritable(true)
            }
        }

    @Test
    fun aFileWithFieldsFromANewerVersionStillLoads() =
        runTest {
            store().save(VALID_ID, conversation())
            val file = File(dir, "$VALID_ID.json")
            file.writeText(file.readText().replaceFirst("{", "{\"addedLater\":{\"nested\":[1,2]},"))

            assertEquals(conversation(), store().load(VALID_ID)?.conversation)
        }

    @Test
    fun reopeningStraightAfterAnUnfinishedSaveReadsTheNewVersion() =
        runBlocking {
            val slow = AtomicBoolean(false)
            val saving = CountDownLatch(1)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store =
                FileSavedChatStore(dir, Dispatchers.IO, scope, clock = {
                    if (slow.get()) {
                        saving.countDown()
                        Thread.sleep(SLOW_WRITE_MILLIS)
                    }
                    now
                })
            store.save(VALID_ID, conversation(lines = listOf("Old")))
            slow.set(true)
            val pending = scope.launch { store.save(VALID_ID, conversation(lines = listOf("New"))) }
            saving.await()

            assertEquals(conversation(lines = listOf("New")), store.load(VALID_ID)?.conversation)
            pending.join()
            scope.cancel()
        }

    @Test
    fun interleavedSavesAndDeletesLeaveTheListMatchingTheFiles() =
        runBlocking {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = FileSavedChatStore(dir, Dispatchers.IO, scope, clock = { now })
            val ids = listOf(VALID_ID, OTHER_VALID_ID)
            (0 until INTERLEAVED_OPERATIONS)
                .map { index ->
                    val id = ids[index % ids.size]
                    scope.launch { if (index % 3 == 0) store.delete(id) else store.save(id, conversation(lines = listOf("Line $index"))) }
                }.joinAll()

            val onDisk =
                dir
                    .listFiles()
                    .orEmpty()
                    .map { it.name.removeSuffix(".json") }
                    .toSet()
            assertEquals(
                onDisk,
                store.summaries.value
                    .map { it.id }
                    .toSet(),
            )
            scope.cancel()
        }

    @Test
    fun loadingAChatWhoseFileHasGoneDropsItFromTheList() =
        runTest {
            val store = store()
            store.save(VALID_ID, conversation())
            File(dir, "$VALID_ID.json").delete()

            assertNull(store.load(VALID_ID))
            assertEquals(emptyList(), store.summaries.value)
        }

    @Test
    fun loadingAChatThatHasBecomeUnreadableMarksItSo() =
        runTest {
            val store = store()
            store.save(VALID_ID, conversation())
            File(dir, "$VALID_ID.json").writeText("not json")

            assertNull(store.load(VALID_ID))
            assertEquals(listOf(false), store.summaries.value.map { it.readable })
        }
}

private const val SLOW_WRITE_MILLIS = 300L
private const val INTERLEAVED_OPERATIONS = 60
