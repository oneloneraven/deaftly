package com.ravendarque.deaftly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkDeaftlyColors else LightDeaftlyColors
    CompositionLocalProvider(
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
