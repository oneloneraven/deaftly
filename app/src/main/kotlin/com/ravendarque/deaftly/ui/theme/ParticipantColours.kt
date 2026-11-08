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
)

private fun tone(
    container: Long,
    onContainer: Long,
    onContainerMuted: Long,
    strong: Long,
    onStrong: Long,
) = ParticipantColours(Color(container), Color(onContainer), Color(onContainerMuted), Color(strong), Color(onStrong))

private val LightTeal = tone(0xFF2FA392, 0xFF04201B, 0xFF04302A, 0xFF1F7F70, 0xFFFFFFFF)
private val DarkTeal = tone(0xFF35998A, 0xFF04201B, 0xFF02241F, 0xFF35998A, 0xFF04201B)
private val LightRose = tone(0xFFEE8FA9, 0xFF3A0A1C, 0xFF5C1530, 0xFFC2416A, 0xFFFFFFFF)
private val DarkRose = tone(0xFFA83A5F, 0xFFFFFFFF, 0xFFFFD6E1, 0xFFE8728F, 0xFF2A0610)
private val LightBerry = tone(0xFF5E1850, 0xFFFFFFFF, 0xFFF3D3EA, 0xFF5E1850, 0xFFFFFFFF)
private val DarkBerry = tone(0xFFEBC4F7, 0xFF2A0F33, 0xFF4A1E58, 0xFFEBC4F7, 0xFF2A0F33)

fun ColourRole.colours(
    theme: DeaftlyColors,
    dark: Boolean,
): ParticipantColours =
    when (this) {
        ColourRole.Accent -> {
            ParticipantColours(theme.personA, theme.onPersonA, theme.onAccentMuted, theme.accent, theme.onAccent)
        }

        ColourRole.PersonB -> {
            ParticipantColours(theme.personB, theme.onPersonB, theme.onPersonBMuted, theme.personBStrong, theme.onPersonBStrong)
        }

        ColourRole.Teal -> {
            if (dark) DarkTeal else LightTeal
        }

        ColourRole.Rose -> {
            if (dark) DarkRose else LightRose
        }

        ColourRole.Berry -> {
            if (dark) DarkBerry else LightBerry
        }
    }

@Composable
fun ColourRole.colours(): ParticipantColours = colours(Deaftly.colors, LocalDarkTheme.current)
