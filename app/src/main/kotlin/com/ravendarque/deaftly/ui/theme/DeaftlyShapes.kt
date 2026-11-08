package com.ravendarque.deaftly.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class DeaftlyShapes(
    val chip: Shape = RoundedCornerShape(8.dp),
    val pill: Shape = CircleShape,
    val controlSmall: Shape = RoundedCornerShape(14.dp),
    val control: Shape = RoundedCornerShape(16.dp),
    val cardSmall: Shape = RoundedCornerShape(18.dp),
    val card: Shape = RoundedCornerShape(22.dp),
    val cardLarge: Shape = RoundedCornerShape(24.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
)

val LocalDeaftlyShapes = staticCompositionLocalOf { DeaftlyShapes() }

fun materialShapes(): Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
