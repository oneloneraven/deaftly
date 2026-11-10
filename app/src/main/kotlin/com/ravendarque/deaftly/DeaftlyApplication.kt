package com.ravendarque.deaftly

import android.app.Application
import android.os.Build
import com.ravendarque.deaftly.ui.theme.PlatformNightMode
import com.ravendarque.deaftly.ui.theme.ThemeModeSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DeaftlyApplication : Application() {
    val container = AppContainer(this)

    override fun onCreate() {
        super.onCreate()
        val sync = ThemeModeSync(container.settingsRepository, PlatformNightMode(this))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) sync.applyStored()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch { sync.follow() }
    }
}
