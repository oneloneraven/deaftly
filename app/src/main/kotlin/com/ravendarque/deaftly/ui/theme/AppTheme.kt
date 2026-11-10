package com.ravendarque.deaftly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.ravendarque.deaftly.core.settings.ThemeMode

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun AppTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    AppTheme(darkTheme = mode.isDark(isSystemInDarkTheme()), content = content)
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkDeaftlyColors else LightDeaftlyColors
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalDeaftlyColors provides colors,
        LocalDeaftlyShapes provides DeaftlyShapes(),
        LocalDeaftlySpacing provides DeaftlySpacing(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = DeaftlyTypography,
            shapes = materialShapes(),
            content = content,
        )
    }
}
