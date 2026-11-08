package com.ravendarque.deaftly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ravendarque.deaftly.navigation.AppNavigation
import com.ravendarque.deaftly.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DeaftlyApplication).container
        setContent {
            AppTheme {
                AppNavigation(container)
            }
        }
    }
}
