package com.ravendarque.deaftly.ui.theme

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
)

private fun tone(
    container: Long,
    onContainer: Long,
    onContainerMuted: Long,
    strong: Long,
    onStrong: Long,
) = ParticipantColours(Color(container), Color(onContainer), Color(onContainerMuted), Color(strong), Color(onStrong))

private val Red = tone(0xFFCD001A, 0xFFFFFFFF, 0xFFFFE3E6, 0xFFCD001A, 0xFFFFFFFF)
private val Orange = tone(0xFFEF6A00, 0xFF15151A, 0xFF3A1A00, 0xFFEF6A00, 0xFF15151A)
private val Gold = tone(0xFFF2CD00, 0xFF15151A, 0xFF4A3A00, 0xFFF2CD00, 0xFF15151A)
private val Green = tone(0xFF79C300, 0xFF15151A, 0xFF253D00, 0xFF79C300, 0xFF15151A)
private val Blue = tone(0xFF1961AE, 0xFFFFFFFF, 0xFFE1ECF9, 0xFF1961AE, 0xFFFFFFFF)
private val Indigo = tone(0xFF61007D, 0xFFFFFFFF, 0xFFEBCDF5, 0xFF61007D, 0xFFFFFFFF)

fun ColourRole.colours(): ParticipantColours =
    when (this) {
        ColourRole.Red -> Red
        ColourRole.Orange -> Orange
        ColourRole.Gold -> Gold
        ColourRole.Green -> Green
        ColourRole.Blue -> Blue
        ColourRole.Indigo -> Indigo
    }
