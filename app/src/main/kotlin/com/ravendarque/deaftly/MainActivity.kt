package com.ravendarque.deaftly

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ravendarque.deaftly.navigation.AppNavigation
import com.ravendarque.deaftly.navigation.AppShortcut
import com.ravendarque.deaftly.ui.show.LocalVolumeKeys
import com.ravendarque.deaftly.ui.show.VolumeKeys
import com.ravendarque.deaftly.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {
    private var shortcut by mutableStateOf<AppShortcut?>(null)
    private val volumeKeys = VolumeKeys()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppShortcut.publish(this)
        if (savedInstanceState == null) shortcut = AppShortcut.from(intent)
        val container = (application as DeaftlyApplication).container
        setContent {
            AppTheme {
                CompositionLocalProvider(LocalVolumeKeys provides volumeKeys) {
                    AppNavigation(container, shortcut?.route, onShortcutHandled = { shortcut = null })
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = volumeKeys.handle(event) || super.dispatchKeyEvent(event)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppShortcut.from(intent)?.let { shortcut = it }
    }
}
