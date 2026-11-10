package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.core.settings.ShowScheme
import com.ravendarque.deaftly.core.settings.ThemeMode
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
    fun followsTheSystemThemeUntilOneIsChosen() =
        runTest {
            assertEquals(ThemeMode.System, repository(backgroundScope).themeMode.first())
        }

    @Test
    fun theChosenThemeSurvivesAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).setThemeMode(ThemeMode.Dark)
            firstRun.cancelAndJoin()

            assertEquals(ThemeMode.Dark, repository(backgroundScope).themeMode.first())
        }

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
    fun firstRunIsNotDoneUntilMarkedAndThenStaysDone() =
        runTest {
            val firstRun = Job()
            val before = repository(CoroutineScope(firstRun))
            assertEquals(false, before.firstRunDone.first())
            before.setFirstRunDone()
            firstRun.cancelAndJoin()

            assertEquals(true, repository(backgroundScope).firstRunDone.first())
        }

    @Test
    fun theChatDisplayDefaultsToMediumTextWithSidesAsTheyWere() =
        runTest {
            val repository = repository(backgroundScope)

            assertEquals(ChatTextSize.Medium, repository.chatTextSize.first())
            assertEquals(false, repository.chatSidesSwapped.first())
        }

    @Test
    fun theChatTextSizeAndSwappedSidesSurviveAFreshStore() =
        runTest {
            val firstRun = Job()
            val before = repository(CoroutineScope(firstRun))
            before.setChatTextSize(ChatTextSize.Large)
            before.setChatSidesSwapped(true)
            firstRun.cancelAndJoin()

            val after = repository(backgroundScope)
            assertEquals(ChatTextSize.Large, after.chatTextSize.first())
            assertEquals(true, after.chatSidesSwapped.first())
        }

    @Test
    fun theChatDisplaySettingsDoNotOverwriteTheProfile() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setChatProfile(ChatProfile("Alex", ColourRole.Blue))
            repository.setChatTextSize(ChatTextSize.Small)
            repository.setChatSidesSwapped(true)

            assertEquals(ChatProfile("Alex", ColourRole.Blue), repository.chatProfile.first())
        }
}
