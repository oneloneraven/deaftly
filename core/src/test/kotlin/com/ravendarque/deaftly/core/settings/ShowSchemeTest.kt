package com.ravendarque.deaftly.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ShowSchemeTest {
    @Test
    fun cyclesThroughEverySchemeAndBack() {
        assertEquals(ShowScheme.WhiteOnBlack, ShowScheme.BlackOnWhite.next())
        assertEquals(ShowScheme.YellowOnBlack, ShowScheme.WhiteOnBlack.next())
        assertEquals(ShowScheme.BlackOnWhite, ShowScheme.YellowOnBlack.next())
    }
}
