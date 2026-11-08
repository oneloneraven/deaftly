package com.ravendarque.deaftly.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ravendarque.deaftly.core.settings.ShowScheme

@Immutable
data class ShowColours(
    val background: Color,
    val text: Color,
    val muted: Color,
)

private val BlackOnWhite = ShowColours(background = Color(0xFFFFFFFF), text = Color(0xFF000000), muted = Color(0xFF55555E))

private val WhiteOnBlack = ShowColours(background = Color(0xFF000000), text = Color(0xFFFFFFFF), muted = Color(0xFFA9A9B3))

private val YellowOnBlack = ShowColours(background = Color(0xFF000000), text = Color(0xFFFFE15C), muted = Color(0xFFBDB27A))

val ShowScheme.colours: ShowColours
    get() =
        when (this) {
            ShowScheme.BlackOnWhite -> BlackOnWhite
            ShowScheme.WhiteOnBlack -> WhiteOnBlack
            ShowScheme.YellowOnBlack -> YellowOnBlack
        }

fun defaultShowScheme(darkTheme: Boolean): ShowScheme = if (darkTheme) ShowScheme.WhiteOnBlack else ShowScheme.BlackOnWhite
