package com.ravendarque.deaftly.ui.theme

import com.ravendarque.deaftly.core.settings.ThemeMode
import com.ravendarque.deaftly.ui.chat.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeModeSyncTest {
    private val applied = mutableListOf<ThemeMode>()
    private val applier = NightModeApplier { applied += it }

    @Test
    fun appliesTheStoredModeAtStartup() {
        ThemeModeSync(FakeSettingsRepository(theme = ThemeMode.Dark), applier).applyStored()

        assertEquals(listOf(ThemeMode.Dark), applied)
    }

    @Test
    fun appliesSystemWhenNothingWasEverChosen() {
        ThemeModeSync(FakeSettingsRepository(), applier).applyStored()

        assertEquals(listOf(ThemeMode.System), applied)
    }

    @Test
    fun followsLaterChangesWithoutReapplyingTheStartupValue() =
        runTest {
            val settings = FakeSettingsRepository(theme = ThemeMode.Dark)
            val sync = ThemeModeSync(settings, applier)
            sync.applyStored()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sync.follow() }

            settings.setThemeMode(ThemeMode.Light)
            settings.setThemeMode(ThemeMode.System)

            assertEquals(listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.System), applied)
        }
}
