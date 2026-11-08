package com.ravendarque.deaftly.core.contrast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WcagContrastTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun luminanceOfBlackAndWhiteIsZeroAndOne() {
        assertEquals(0.0, WcagContrast.relativeLuminance(black), 1e-9)
        assertEquals(1.0, WcagContrast.relativeLuminance(white), 1e-9)
    }

    @Test
    fun luminanceWeightsEachChannel() {
        assertEquals(0.2126, WcagContrast.relativeLuminance(0xFFFF0000.toInt()), 1e-9)
        assertEquals(0.7152, WcagContrast.relativeLuminance(0xFF00FF00.toInt()), 1e-9)
        assertEquals(0.0722, WcagContrast.relativeLuminance(0xFF0000FF.toInt()), 1e-9)
    }

    @Test
    fun blackOnWhiteIsTwentyOneToOne() {
        assertEquals(21.0, WcagContrast.contrastRatio(black, white), 1e-9)
    }

    @Test
    fun ratioIsSymmetric() {
        assertEquals(
            WcagContrast.contrastRatio(white, black),
            WcagContrast.contrastRatio(black, white),
            1e-12,
        )
    }

    @Test
    fun identicalColoursAreOneToOne() {
        assertEquals(1.0, WcagContrast.contrastRatio(0xFF3D4ED8.toInt(), 0xFF3D4ED8.toInt()), 1e-12)
    }

    @Test
    fun matchesPublishedGreyOnWhiteValues() {
        assertEquals(4.54, WcagContrast.contrastRatio(0xFF767676.toInt(), white), 0.005)
        assertEquals(4.48, WcagContrast.contrastRatio(0xFF777777.toInt(), white), 0.005)
    }

    @Test
    fun aaBoundaryForNormalText() {
        assertTrue(WcagContrast.meetsAa(0xFF767676.toInt(), white))
        assertFalse(WcagContrast.meetsAa(0xFF777777.toInt(), white))
    }

    @Test
    fun largeTextNeedsOnlyThreeToOne() {
        val grey = 0xFF949494.toInt()
        assertTrue(WcagContrast.meetsAa(grey, white, largeText = true))
        assertFalse(WcagContrast.meetsAa(grey, white))
    }

    @Test
    fun translucentColoursAreRejected() {
        assertFailsWith<IllegalArgumentException> { WcagContrast.relativeLuminance(0x80FFFFFF.toInt()) }
    }
}
