package com.ravendarque.deaftly.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ShowTextSizeTest {
    @Test
    fun largeIsTheOriginalShowSizeWithTwoSmallerSteps() {
        assertEquals(listOf(36, 46, 56), ShowTextSize.entries.map { it.sp })
    }

    @Test
    fun mediumIsTheDefault() {
        assertEquals(ShowTextSize.Medium, ShowTextSize.Default)
    }

    @Test
    fun stepsUpAndDownAndStopsAtEitherEnd() {
        assertEquals(ShowTextSize.Medium, ShowTextSize.Small.larger())
        assertEquals(ShowTextSize.Large, ShowTextSize.Medium.larger())
        assertEquals(ShowTextSize.Large, ShowTextSize.Large.larger())
        assertEquals(ShowTextSize.Medium, ShowTextSize.Large.smaller())
        assertEquals(ShowTextSize.Small, ShowTextSize.Medium.smaller())
        assertEquals(ShowTextSize.Small, ShowTextSize.Small.smaller())
    }
}
