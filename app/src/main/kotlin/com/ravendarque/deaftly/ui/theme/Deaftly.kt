package com.ravendarque.deaftly.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

object Deaftly {
    val colors: DeaftlyColors
        @Composable @ReadOnlyComposable
        get() = LocalDeaftlyColors.current

    val shapes: DeaftlyShapes
        @Composable @ReadOnlyComposable
        get() = LocalDeaftlyShapes.current

    val spacing: DeaftlySpacing
        @Composable @ReadOnlyComposable
        get() = LocalDeaftlySpacing.current
}
