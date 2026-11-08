package com.ravendarque.deaftly.core.contrast

import kotlin.math.pow

public object WcagContrast {
    public const val AA_NORMAL_TEXT: Double = 4.5
    public const val AA_LARGE_TEXT: Double = 3.0

    private const val OPAQUE: Int = 0xFF
    private const val ALPHA_SHIFT: Int = 24
    private const val RED_SHIFT: Int = 16
    private const val GREEN_SHIFT: Int = 8
    private const val CHANNEL_MAX: Double = 255.0
    private const val LINEAR_THRESHOLD: Double = 0.04045
    private const val LINEAR_DIVISOR: Double = 12.92
    private const val GAMMA_OFFSET: Double = 0.055
    private const val GAMMA_SCALE: Double = 1.055
    private const val GAMMA: Double = 2.4
    private const val RED_WEIGHT: Double = 0.2126
    private const val GREEN_WEIGHT: Double = 0.7152
    private const val BLUE_WEIGHT: Double = 0.0722
    private const val FLARE: Double = 0.05

    public fun relativeLuminance(argb: Int): Double {
        require(argb ushr ALPHA_SHIFT == OPAQUE) { "Colour must be fully opaque" }
        val red = linear(argb shr RED_SHIFT and OPAQUE)
        val green = linear(argb shr GREEN_SHIFT and OPAQUE)
        val blue = linear(argb and OPAQUE)
        return RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue
    }

    public fun contrastRatio(
        foreground: Int,
        background: Int,
    ): Double {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (maxOf(a, b) + FLARE) / (minOf(a, b) + FLARE)
    }

    public fun meetsAa(
        foreground: Int,
        background: Int,
        largeText: Boolean = false,
    ): Boolean = contrastRatio(foreground, background) >= if (largeText) AA_LARGE_TEXT else AA_NORMAL_TEXT

    private fun linear(channel: Int): Double {
        val c = channel / CHANNEL_MAX
        return if (c <= LINEAR_THRESHOLD) c / LINEAR_DIVISOR else ((c + GAMMA_OFFSET) / GAMMA_SCALE).pow(GAMMA)
    }
}
