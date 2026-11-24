package com.oneloneraven.deaftly.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oneloneraven.deaftly.AppContainer
import com.oneloneraven.deaftly.DeaftlyApplication
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val LOAD_TIMEOUT_MILLIS = 10_000L

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w390dp-h800dp")
class AppShortcutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun container(): AppContainer = (composeRule.activity.application as DeaftlyApplication).container

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun waitForText(id: Int) =
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(id)).fetchSemanticsNodes().isNotEmpty()
        }

    private fun waitForDescription(id: Int) =
        composeRule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithContentDescription(text(id)).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun publishesOneShortcutPerEntryThatOpensTheApp() {
        val context = composeRule.activity
        AppShortcut.publish(context)

        val published = ShortcutManagerCompat.getDynamicShortcuts(context)
        assertEquals(AppShortcut.entries.map { it.id }, published.sortedBy { it.rank }.map { it.id })
        published.forEach { info ->
            assertEquals(AppShortcut.entries.first { it.id == info.id }, AppShortcut.from(info.intent))
        }
    }

    @Test
    fun eachHubFeatureMapsToItsShortcut() {
        assertEquals(AppShortcut.SideBySide, AppShortcut.forRoute(Route.Chat()))
        assertEquals(AppShortcut.Listen, AppShortcut.forRoute(Route.Listen))
        assertNull(AppShortcut.forRoute(Route.Templates))
    }

    @Test
    fun ignoresIntentsThatAreNotShortcuts() {
        assertNull(AppShortcut.from(Intent(Intent.ACTION_MAIN)))
        assertNull(AppShortcut.from(Intent().putExtra("com.oneloneraven.deaftly.extra.SHORTCUT", "unknown")))
        assertNull(AppShortcut.from(null))
    }

    @Test
    fun shortcutOpensItsScreenOverTheHub() {
        runBlocking { container().settingsRepository.setFirstRunDone() }
        composeRule.setContent { AppTheme { AppNavigation(container(), AppShortcut.FaceToFace.route) } }

        waitForDescription(R.string.f2f_close)
        pressBack()
        waitForText(R.string.hub_start_typing)
    }

    @Test
    fun shortcutArrivingWhileOpenIsAddedOnTop() {
        runBlocking { container().settingsRepository.setFirstRunDone() }
        var shortcut by mutableStateOf<Route?>(null)
        composeRule.setContent { AppTheme { AppNavigation(container(), shortcut, onShortcutHandled = { shortcut = null }) } }
        waitForText(R.string.hub_start_typing)

        shortcut = AppShortcut.FaceToFace.route
        waitForDescription(R.string.f2f_close)
        assertNull(shortcut)
    }

    @Test
    fun shortcutWaitsForFirstRunToFinish() {
        composeRule.setContent { AppTheme { AppNavigation(container(), AppShortcut.FaceToFace.route) } }

        waitForText(R.string.first_run_start)
        assertEquals(0, composeRule.onAllNodesWithContentDescription(text(R.string.f2f_close)).fetchSemanticsNodes().size)
    }
}
