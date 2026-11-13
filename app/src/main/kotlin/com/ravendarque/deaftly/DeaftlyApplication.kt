package com.ravendarque.deaftly

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.ravendarque.deaftly.ui.theme.PlatformNightMode
import com.ravendarque.deaftly.ui.theme.ThemeModeSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DeaftlyApplication : Application() {
    val container = AppContainer(this)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var locales: LocaleList? = null

    override fun onCreate() {
        super.onCreate()
        locales = resources.configuration.locales
        val sync = ThemeModeSync(container.settingsRepository, PlatformNightMode(this))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) sync.applyStored()
        scope.launch { sync.follow() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.locales != locales) {
            locales = newConfig.locales
            scope.launch { container.refreshBuiltInTemplates() }
        }
    }
}
