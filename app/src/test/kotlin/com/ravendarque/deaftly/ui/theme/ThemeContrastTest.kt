package com.ravendarque.deaftly.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ravendarque.deaftly.core.contrast.WcagContrast
import org.junit.Test
import kotlin.test.assertTrue

class ThemeContrastTest {
    private data class ColourPair(
        val name: String,
        val foreground: Color,
        val background: Color,
    )

    private fun tokenPairs(c: DeaftlyColors) =
        listOf(
            ColourPair("ink on ground", c.ink, c.ground),
            ColourPair("ink on surface", c.ink, c.surface),
            ColourPair("ink on surfaceSunken", c.ink, c.surfaceSunken),
            ColourPair("inkSecondary on ground", c.inkSecondary, c.ground),
            ColourPair("inkSecondary on surface", c.inkSecondary, c.surface),
            ColourPair("inkMuted on ground", c.inkMuted, c.ground),
            ColourPair("inkMuted on surface", c.inkMuted, c.surface),
            ColourPair("inkMuted on surfaceSunken", c.inkMuted, c.surfaceSunken),
            ColourPair("ground on ink", c.ground, c.ink),
            ColourPair("accent on ground", c.accent, c.ground),
            ColourPair("accent on surface", c.accent, c.surface),
            ColourPair("accentStrong on ground", c.accentStrong, c.ground),
            ColourPair("accentStrong on surface", c.accentStrong, c.surface),
            ColourPair("onAccent on accent", c.onAccent, c.accent),
            ColourPair("onAccentMuted on accent", c.onAccentMuted, c.accent),
            ColourPair("onAccentSoft on accentSoft", c.onAccentSoft, c.accentSoft),
            ColourPair("accentStrong on accentSoft", c.accentStrong, c.accentSoft),
            ColourPair("ink on accentSoft", c.ink, c.accentSoft),
            ColourPair("onPersonA on personA", c.onPersonA, c.personA),
            ColourPair("onPersonB on personB", c.onPersonB, c.personB),
            ColourPair("onPersonBMuted on personB", c.onPersonBMuted, c.personB),
            ColourPair("onPersonBStrong on personBStrong", c.onPersonBStrong, c.personBStrong),
            ColourPair("onHighlight on highlight", c.onHighlight, c.highlight),
            ColourPair("onError on error", c.onError, c.error),
            ColourPair("onErrorContainer on errorContainer", c.onErrorContainer, c.errorContainer),
        )

    private fun materialPairs(
        c: DeaftlyColors,
        dark: Boolean,
    ) = c.toMaterialColorScheme(dark).let { s ->
        listOf(
            ColourPair("onPrimary on primary", s.onPrimary, s.primary),
            ColourPair("onPrimaryContainer on primaryContainer", s.onPrimaryContainer, s.primaryContainer),
            ColourPair("onSecondary on secondary", s.onSecondary, s.secondary),
            ColourPair("onSecondaryContainer on secondaryContainer", s.onSecondaryContainer, s.secondaryContainer),
            ColourPair("onTertiary on tertiary", s.onTertiary, s.tertiary),
            ColourPair("onTertiaryContainer on tertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer),
            ColourPair("onBackground on background", s.onBackground, s.background),
            ColourPair("onSurface on surface", s.onSurface, s.surface),
            ColourPair("onSurface on surfaceContainer", s.onSurface, s.surfaceContainer),
            ColourPair("onSurface on surfaceContainerHighest", s.onSurface, s.surfaceContainerHighest),
            ColourPair("onSurfaceVariant on surface", s.onSurfaceVariant, s.surface),
            ColourPair("onSurfaceVariant on surfaceVariant", s.onSurfaceVariant, s.surfaceVariant),
            ColourPair("inverseOnSurface on inverseSurface", s.inverseOnSurface, s.inverseSurface),
            ColourPair("inversePrimary on inverseSurface", s.inversePrimary, s.inverseSurface),
            ColourPair("onError on error", s.onError, s.error),
            ColourPair("onErrorContainer on errorContainer", s.onErrorContainer, s.errorContainer),
        )
    }

    private fun assertAllMeetAa(
        theme: String,
        pairs: List<ColourPair>,
    ) {
        val failures =
            pairs.filterNot { WcagContrast.meetsAa(it.foreground.toArgb(), it.background.toArgb()) }
        assertTrue(
            failures.isEmpty(),
            failures.joinToString(prefix = "$theme below 4.5:1: ") {
                "${it.name} (%.2f)".format(WcagContrast.contrastRatio(it.foreground.toArgb(), it.background.toArgb()))
            },
        )
    }

    @Test
    fun lightTokenPairsMeetAa() = assertAllMeetAa("light", tokenPairs(LightDeaftlyColors))

    @Test
    fun darkTokenPairsMeetAa() = assertAllMeetAa("dark", tokenPairs(DarkDeaftlyColors))

    @Test
    fun lightSchemeMeetsAa() = assertAllMeetAa("light", materialPairs(LightDeaftlyColors, dark = false))

    @Test
    fun darkSchemeMeetsAa() = assertAllMeetAa("dark", materialPairs(DarkDeaftlyColors, dark = true))
}
