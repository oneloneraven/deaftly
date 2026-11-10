package com.ravendarque.deaftly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ravendarque.deaftly.navigation.AppNavigation
import com.ravendarque.deaftly.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {
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
