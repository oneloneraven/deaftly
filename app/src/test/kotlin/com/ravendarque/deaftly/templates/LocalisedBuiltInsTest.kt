package com.ravendarque.deaftly.templates

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.l10n.resourcesFor
import com.ravendarque.deaftly.storage.BuiltInReconciliation
import com.ravendarque.deaftly.storage.DataStoreTemplateRepository
import com.ravendarque.deaftly.storage.StoreDocument
import com.ravendarque.deaftly.storage.createTemplateStore
import com.ravendarque.deaftly.storage.refreshBuiltIns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LocalisedBuiltInsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "templates.json") }

    private fun shippedFor(qualifiers: String) = builtInTemplates(resourcesFor(qualifiers))

    private fun List<Template>.byId(id: String) = first { it.id == id }

    private suspend fun launch(
        shipped: List<Template>,
        block: suspend DataStoreTemplateRepository.() -> Unit,
    ) {
        val run = Job()
        DataStoreTemplateRepository(createTemplateStore(file, shipped, CoroutineScope(run)), { shipped }).block()
        run.cancelAndJoin()
    }

    private suspend fun DataStoreTemplateRepository.all() = templates.first()

    @Test
    fun everyLocaleNamesItsOwnSignLanguageInTheInterpreterTemplate() {
        mapOf(
            "en-rGB" to "BSL",
            "en-rUS" to "ASL",
            "en-rCA" to "ASL",
            "en-rAU" to "Auslan",
            "en-rNZ" to "NZSL",
            "en-rIE" to "ISL",
        ).forEach { (qualifiers, language) ->
            val templates = shippedFor(qualifiers)
            val interpreter = templates.byId(BuiltInIds.BSL_INTERPRETER)
            val article = if (language == "BSL") "a" else "an"

            assertEquals("$language interpreter", interpreter.title, qualifiers)
            assertEquals(
                "Hi! I'm deaf. My name is {name}. I should have $article $language interpreter booked. " +
                    "Are they here yet? Please {how to reply}.",
                interpreter.body,
                qualifiers,
            )
            assertEquals(
                listOf("$language interpreter", "speech to text", "typing on my phone", "lipreading"),
                templates
                    .byId(BuiltInIds.DOCTOR)
                    .placeholders
                    .getValue("how I communicate")
                    .options,
                qualifiers,
            )
        }
    }

    @Test
    fun anUneditedBuiltInUpdatesWhenTheStoreIsOpenedInAnotherLocale() =
        runTest {
            val british = shippedFor("en-rGB")
            val american = shippedFor("en-rUS")
            launch(british) { setPinned(BuiltInIds.ASK_HELP, true) }

            launch(american) {
                val result = all()

                assertEquals(american.byId(BuiltInIds.BSL_INTERPRETER), result.byId(BuiltInIds.BSL_INTERPRETER))
                assertEquals(american.byId(BuiltInIds.PHARMACY), result.byId(BuiltInIds.PHARMACY))
                assertTrue(
                    "the restrooms" in
                        result
                            .byId(BuiltInIds.ASK_HELP)
                            .placeholders
                            .getValue("thing")
                            .options,
                )
            }
        }

    @Test
    fun thePinnedStateSurvivesTheLocaleChange() =
        runTest {
            val british = shippedFor("en-rGB")
            val american = shippedFor("en-rUS")
            launch(british) {
                setPinned(BuiltInIds.ASK_HELP, true)
                setPinned(BuiltInIds.CHECK_IN, false)
            }

            launch(american) {
                val result = all()

                assertTrue(result.byId(BuiltInIds.ASK_HELP).pinned)
                assertFalse(result.byId(BuiltInIds.CHECK_IN).pinned)
                assertEquals(american.byId(BuiltInIds.ASK_HELP).copy(pinned = true), result.byId(BuiltInIds.ASK_HELP))
            }
        }

    @Test
    fun anEditedBuiltInKeepsItsWordingWhenTheLocaleChanges() =
        runTest {
            val british = shippedFor("en-rGB")
            val edited = british.byId(BuiltInIds.BSL_INTERPRETER).copy(title = "My interpreter")
            launch(british) { update(edited) }

            launch(shippedFor("en-rUS")) {
                assertEquals(edited, all().byId(BuiltInIds.BSL_INTERPRETER))
            }
        }

    @Test
    fun aHiddenBuiltInStaysHiddenWhenTheLocaleChanges() =
        runTest {
            launch(shippedFor("en-rGB")) { delete(BuiltInIds.PHARMACY) }

            launch(shippedFor("en-rUS")) {
                assertTrue(all().none { it.id == BuiltInIds.PHARMACY })
                assertEquals(1, hiddenBuiltInCount.first())
            }
        }

    @Test
    fun aLocaleChangeWhileTheAppIsRunningUpdatesUneditedBuiltIns() =
        runTest {
            val british = shippedFor("en-rGB")
            val american = shippedFor("en-rUS")
            var shipped = british
            val edited = british.byId(BuiltInIds.DOCTOR).copy(title = "My doctor")
            val run = Job()
            val store = createTemplateStore(file, british, CoroutineScope(run))
            val repository = DataStoreTemplateRepository(store, { shipped })
            repository.update(edited)
            repository.delete(BuiltInIds.PHARMACY)
            repository.setPinned(BuiltInIds.ASK_HELP, true)

            shipped = american
            store.refreshBuiltIns(shipped)
            val result = repository.all()

            assertEquals(american.byId(BuiltInIds.BSL_INTERPRETER), result.byId(BuiltInIds.BSL_INTERPRETER))
            assertEquals(edited, result.byId(BuiltInIds.DOCTOR))
            assertTrue(result.byId(BuiltInIds.ASK_HELP).pinned)
            assertTrue(result.none { it.id == BuiltInIds.PHARMACY })
            assertEquals(1, repository.hiddenBuiltInCount.first())

            shipped = british
            store.refreshBuiltIns(shipped)

            assertEquals(british.byId(BuiltInIds.BSL_INTERPRETER), repository.all().byId(BuiltInIds.BSL_INTERPRETER))
            run.cancelAndJoin()
        }

    @Test
    fun restoringABuiltInUsesTheCurrentLocale() =
        runTest {
            var shipped = shippedFor("en-rGB")
            val run = Job()
            val repository = DataStoreTemplateRepository(createTemplateStore(file, shipped, CoroutineScope(run)), { shipped })
            repository.delete(BuiltInIds.BSL_INTERPRETER)

            shipped = shippedFor("en-rAU")
            repository.restoreHiddenBuiltIns()

            assertEquals("Auslan interpreter", repository.all().byId(BuiltInIds.BSL_INTERPRETER).title)
            run.cancelAndJoin()
        }

    @Test
    fun theMigrationRunsOnlyWhenTheShippedContentDiffersFromTheStore() =
        runTest {
            val british = shippedFor("en-rGB")
            val stored = StoreDocument.seeded(british)

            assertFalse(BuiltInReconciliation(british).shouldMigrate(stored))
            assertTrue(BuiltInReconciliation(shippedFor("en-rUS")).shouldMigrate(stored))
            assertTrue(BuiltInReconciliation(shippedFor("en-rAU")).shouldMigrate(stored))
            assertTrue(BuiltInReconciliation(shippedFor("en-rIE")).shouldMigrate(stored))
        }
}
