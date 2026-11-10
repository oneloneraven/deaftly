package com.ravendarque.deaftly

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.navigation.AppNavigation
import com.ravendarque.deaftly.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DeaftlyApplication).container
        setContent {
            val mode by container.settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = null)
            mode?.let { chosen ->
                val dark = chosen.isDark(isSystemInDarkTheme())
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(Color.WHITE, Color.BLACK) { dark },
                    )
                    onDispose {}
                }
                AppTheme(chosen) { AppNavigation(container) }
            }
        }
    }
}
