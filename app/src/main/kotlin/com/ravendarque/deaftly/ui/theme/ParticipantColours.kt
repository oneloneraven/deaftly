package com.ravendarque.deaftly.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ravendarque.deaftly.core.chat.ColourRole

@Immutable
data class ParticipantColours(
    val container: Color,
    val onContainer: Color,
    val onContainerMuted: Color,
    val strong: Color,
    val onStrong: Color,
    val label: Color,
)

private class Palette(
    val container: Long,
    val onContainer: Long,
    val onContainerMuted: Long,
    val lightLabel: Long,
    val darkLabel: Long,
) {
    fun colours(dark: Boolean) =
        ParticipantColours(
            container = Color(container),
            onContainer = Color(onContainer),
            onContainerMuted = Color(onContainerMuted),
            strong = Color(container),
            onStrong = Color(onContainer),
            label = Color(if (dark) darkLabel else lightLabel),
        )
}

private fun ColourRole.palette(): Palette =
    when (this) {
        ColourRole.Red -> Palette(0xFFCD001A, 0xFFFFFFFF, 0xFFFFE3E6, 0xFFCD001A, 0xFFFF6B7C)
        ColourRole.Orange -> Palette(0xFFEF6A00, 0xFF15151A, 0xFF3A1A00, 0xFFA34600, 0xFFFF9340)
        ColourRole.Gold -> Palette(0xFFF2CD00, 0xFF15151A, 0xFF4A3A00, 0xFF7A5E00, 0xFFF2CD00)
        ColourRole.Green -> Palette(0xFF79C300, 0xFF15151A, 0xFF253D00, 0xFF3F6B00, 0xFF92D400)
        ColourRole.Blue -> Palette(0xFF1961AE, 0xFFFFFFFF, 0xFFE1ECF9, 0xFF1961AE, 0xFF6FA8F0)
        ColourRole.Indigo -> Palette(0xFF61007D, 0xFFFFFFFF, 0xFFEBCDF5, 0xFF61007D, 0xFFC896E0)
    }

private val Light = ColourRole.entries.associateWith { it.palette().colours(dark = false) }
private val Dark = ColourRole.entries.associateWith { it.palette().colours(dark = true) }

fun ColourRole.colours(dark: Boolean): ParticipantColours = checkNotNull((if (dark) Dark else Light)[this])

@Composable
fun ColourRole.colours(): ParticipantColours = colours(LocalDarkTheme.current)
