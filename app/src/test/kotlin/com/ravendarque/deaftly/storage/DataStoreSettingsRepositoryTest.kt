package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.ShowScheme
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
import kotlin.test.assertNull

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun repository(scope: CoroutineScope) =
        DataStoreSettingsRepository(createTemplateStore(File(folder.root, "templates.json"), builtIns, scope))

    @Test
    fun hasNoChosenSchemeUntilOneIsSet() =
        runTest {
            assertNull(repository(backgroundScope).showScheme.first())
        }

    @Test
    fun theChosenSchemeSurvivesAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).setShowScheme(ShowScheme.YellowOnBlack)
            firstRun.cancelAndJoin()

            assertEquals(ShowScheme.YellowOnBlack, repository(backgroundScope).showScheme.first())
        }

    @Test
    fun defaultsToNoNameAndTheAccentColourUntilOneIsSet() =
        runTest {
            assertEquals(ChatProfile(null, ColourRole.Indigo), repository(backgroundScope).chatProfile.first())
        }

    @Test
    fun theChatProfileSurvivesAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).setChatProfile(ChatProfile("Alex", ColourRole.Green))
            firstRun.cancelAndJoin()

            assertEquals(ChatProfile("Alex", ColourRole.Green), repository(backgroundScope).chatProfile.first())
        }

    @Test
    fun aBlankNameIsStoredAsNoName() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setChatProfile(ChatProfile("  ", ColourRole.Red))

            assertEquals(ChatProfile(null, ColourRole.Red), repository.chatProfile.first())
        }

    @Test
    fun theChatProfileAndTheShowSchemeDoNotOverwriteEachOther() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setShowScheme(ShowScheme.WhiteOnBlack)
            repository.setChatProfile(ChatProfile("Alex", ColourRole.Blue))

            assertEquals(ShowScheme.WhiteOnBlack, repository.showScheme.first())
            assertEquals(ChatProfile("Alex", ColourRole.Blue), repository.chatProfile.first())
        }

    @Test
    fun savingTheProfileLeavesTheTemplatesAlone() =
        runTest {
            val store = createTemplateStore(File(folder.root, "templates.json"), builtIns, backgroundScope)

            DataStoreSettingsRepository(store).setChatProfile(ChatProfile("Alex", ColourRole.Blue))

            assertEquals(
                builtIns,
                store.data
                    .first()
                    .templates
                    .map { it.toDomain() },
            )
        }

    @Test
    fun defaultsToTheSidesLayoutUntilOneIsChosen() =
        runTest {
            assertEquals(ChatLayout.Sides, repository(backgroundScope).chatLayout.first())
        }

    @Test
    fun theChosenLayoutSurvivesAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).setChatLayout(ChatLayout.Split)
            firstRun.cancelAndJoin()

            assertEquals(ChatLayout.Split, repository(backgroundScope).chatLayout.first())
        }

    @Test
    fun firstRunIsNotDoneUntilMarkedAndThenStaysDone() =
        runTest {
            val firstRun = Job()
            val before = repository(CoroutineScope(firstRun))
            assertEquals(false, before.firstRunDone.first())
            before.setFirstRunDone()
            firstRun.cancelAndJoin()

            assertEquals(true, repository(backgroundScope).firstRunDone.first())
        }
}
