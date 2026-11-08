package com.ravendarque.deaftly.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ShowColours(
    val background: Color,
    val text: Color,
    val muted: Color,
)

val BlackOnWhite = ShowColours(background = Color(0xFFFFFFFF), text = Color(0xFF000000), muted = Color(0xFF55555E))

val WhiteOnBlack = ShowColours(background = Color(0xFF000000), text = Color(0xFFFFFFFF), muted = Color(0xFFA9A9B3))

fun defaultShowColours(darkTheme: Boolean): ShowColours = if (darkTheme) WhiteOnBlack else BlackOnWhite
