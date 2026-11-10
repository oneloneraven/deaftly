package com.ravendarque.deaftly.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.core.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun interface NightModeApplier {
    fun apply(mode: ThemeMode)
}

class PlatformNightMode(
    private val context: Context,
) : NightModeApplier {
    override fun apply(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode.uiMode())
        } else {
            AppCompatDelegate.setDefaultNightMode(mode.compatMode())
        }
    }
}

class ThemeModeSync(
    private val settings: SettingsRepository,
    private val applier: NightModeApplier,
) {
    private var applied: ThemeMode? = null

    fun applyStored() {
        applyOnce(runBlocking { settings.themeMode.first() })
    }

    suspend fun follow() {
        settings.themeMode.collect(::applyOnce)
    }

    private fun applyOnce(mode: ThemeMode) {
        if (mode == applied) return
        applied = mode
        applier.apply(mode)
    }
}

private fun ThemeMode.uiMode(): Int =
    when (this) {
        ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
        ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
    }

private fun ThemeMode.compatMode(): Int =
    when (this) {
        ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
    }
