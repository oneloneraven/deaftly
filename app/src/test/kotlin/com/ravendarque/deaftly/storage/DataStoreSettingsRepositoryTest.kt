package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
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

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun repository(scope: CoroutineScope) =
        DataStoreSettingsRepository(createTemplateStore(File(folder.root, "templates.json"), builtIns, scope))

    @Test
    fun defaultsToNoNameAndTheAccentColourUntilOneIsSet() =
        runTest {
            assertEquals(ChatProfile(null, ColourRole.Accent), repository(backgroundScope).chatProfile.first())
        }

    @Test
    fun theChatProfileSurvivesAFreshStore() =
        runTest {
            val firstRun = Job()
            repository(CoroutineScope(firstRun)).setChatProfile(ChatProfile("Alex", ColourRole.Teal))
            firstRun.cancelAndJoin()

            assertEquals(ChatProfile("Alex", ColourRole.Teal), repository(backgroundScope).chatProfile.first())
        }

    @Test
    fun aBlankNameIsStoredAsNoName() =
        runTest {
            val repository = repository(backgroundScope)

            repository.setChatProfile(ChatProfile("  ", ColourRole.Rose))

            assertEquals(ChatProfile(null, ColourRole.Rose), repository.chatProfile.first())
        }

    @Test
    fun savingTheProfileLeavesTheTemplatesAlone() =
        runTest {
            val store = createTemplateStore(File(folder.root, "templates.json"), builtIns, backgroundScope)

            DataStoreSettingsRepository(store).setChatProfile(ChatProfile("Alex", ColourRole.Berry))

            assertEquals(
                builtIns,
                store.data
                    .first()
                    .templates
                    .map { it.toDomain() },
            )
        }
}
