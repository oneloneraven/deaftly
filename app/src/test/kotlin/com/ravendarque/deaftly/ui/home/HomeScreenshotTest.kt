package com.ravendarque.deaftly.ui.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val state = HomeUiState(Route.entries.filter { it != Route.Home })

    @Test
    fun lightTheme() {
        composeRule.setContent { AppTheme(darkTheme = false) { HomeContent(state, onNavigate = {}) } }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun darkTheme() {
        composeRule.setContent { AppTheme(darkTheme = true) { HomeContent(state, onNavigate = {}) } }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(fontScale = 2f)
    fun largeFontScale() {
        composeRule.setContent { AppTheme(darkTheme = false) { HomeContent(state, onNavigate = {}) } }
        composeRule.onRoot().captureRoboImage()
    }
}
