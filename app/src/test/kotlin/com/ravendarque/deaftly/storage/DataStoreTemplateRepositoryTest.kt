package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.template.Template
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataStoreTemplateRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var counter = 0
    private val mine = Template(id = "mine", title = "Mine", category = "Everyday", body = "Hello {name}")

    private fun repository(scope: CoroutineScope) =
        DataStoreTemplateRepository(
            createTemplateStore(File(folder.root, "templates.json"), builtIns, scope),
            { builtIns },
        ) { "copy-${++counter}" }

    private suspend fun DataStoreTemplateRepository.all() = templates.first()

    @Test
    fun startsWithTheBuiltInTemplates() =
        runTest {
            assertEquals(builtIns, repository(backgroundScope).all())
        }

    @Test
    fun changesPersistInTheFileAndSurviveAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).add(mine)
            firstRun.cancelAndJoin()

            val reopened = repository(backgroundScope)

            assertEquals(builtIns + mine, reopened.all())
        }

    @Test
    fun addRejectsADuplicateId() =
        runTest {
            val repository = repository(backgroundScope)

            assertFailsWith<IllegalArgumentException> { repository.add(greeting) }
            assertEquals(builtIns, repository.all())
        }

    @Test
    fun addStoresAUserTemplateEvenIfMarkedBuiltIn() =
        runTest {
            val repository = repository(backgroundScope)

            repository.add(mine.copy(builtIn = true))

            assertFalse(repository.all().last().builtIn)
        }

    @Test
    fun updateReplacesContentButKeepsTheBuiltInFlag() =
        runTest {
            val repository = repository(backgroundScope)

            repository.update(greeting.copy(title = "Edited", builtIn = false))

            val edited = repository.all().first { it.id == greeting.id }
            assertEquals("Edited", edited.title)
            assertTrue(edited.builtIn)
        }

    @Test
    fun updateOfAnUnknownIdChangesNothing() =
        runTest {
            val repository = repository(backgroundScope)

            repository.update(mine)

            assertEquals(builtIns, repository.all())
        }

    @Test
    fun deleteRemovesOnlyThatTemplate() =
        runTest {
            val repository = repository(backgroundScope)

            repository.delete(menu.id)
            repository.delete("missing")

            assertEquals(listOf(greeting), repository.all())
        }

    @Test
    fun duplicateCopiesAsAnUnpinnedUserTemplateWithTheGivenTitle() =
        runTest {
            val repository = repository(backgroundScope)

            val copy = repository.duplicate(greeting.id, "Say hello (copy)")

            assertEquals(greeting.copy(id = "copy-1", title = "Say hello (copy)", pinned = false, builtIn = false), copy)
            assertEquals(builtIns + copy!!, repository.all())
        }

    @Test
    fun duplicateOfAnUnknownIdReturnsNull() =
        runTest {
            val repository = repository(backgroundScope)

            assertNull(repository.duplicate("missing", "x"))
            assertEquals(builtIns, repository.all())
        }

    @Test
    fun pinAndUnpin() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setPinned(menu.id, true)
            assertTrue(repository.all().first { it.id == menu.id }.pinned)

            repository.setPinned(menu.id, false)
            assertFalse(repository.all().first { it.id == menu.id }.pinned)
        }

    @Test
    fun setCategory() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setCategory(menu.id, "Travel")

            assertEquals("Travel", repository.all().first { it.id == menu.id }.category)
        }

    @Test
    fun restoreBuiltInResetsAnEditedTemplate() =
        runTest {
            val repository = repository(backgroundScope)
            repository.update(greeting.copy(title = "Edited", body = "Changed", pinned = false))

            repository.restoreBuiltIn(greeting.id)

            assertEquals(builtIns, repository.all())
        }

    @Test
    fun restoreBuiltInBringsBackADeletedTemplate() =
        runTest {
            val repository = repository(backgroundScope)
            repository.delete(greeting.id)

            repository.restoreBuiltIn(greeting.id)

            assertEquals(listOf(menu, greeting), repository.all())
        }

    @Test
    fun restoreBuiltInIgnoresUserTemplates() =
        runTest {
            val repository = repository(backgroundScope)
            repository.add(mine)

            repository.restoreBuiltIn(mine.id)

            assertEquals(builtIns + mine, repository.all())
        }

    @Test
    fun deletingABuiltInHidesItAndCountsItAsHidden() =
        runTest {
            val repository = repository(backgroundScope)
            assertEquals(0, repository.hiddenBuiltInCount.first())

            repository.delete(greeting.id)

            assertEquals(listOf(menu), repository.all())
            assertEquals(1, repository.hiddenBuiltInCount.first())
        }

    @Test
    fun restoringHiddenBuiltInsBringsBackOnlyTheMissingOnesAndKeepsEdits() =
        runTest {
            val repository = repository(backgroundScope)
            val edited = menu.copy(title = "My menu", pinned = true)
            repository.update(edited)
            repository.delete(greeting.id)

            repository.restoreHiddenBuiltIns()

            assertEquals(listOf(edited, greeting), repository.all())
            assertEquals(0, repository.hiddenBuiltInCount.first())
        }

    @Test
    fun restoringWhenNothingIsHiddenChangesNothing() =
        runTest {
            val repository = repository(backgroundScope)

            repository.restoreHiddenBuiltIns()

            assertEquals(builtIns, repository.all())
        }

    @Test
    fun hiddenBuiltInsStayHiddenAcrossAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).delete(greeting.id)
            firstRun.cancelAndJoin()

            assertEquals(1, repository(backgroundScope).hiddenBuiltInCount.first())
        }
}
