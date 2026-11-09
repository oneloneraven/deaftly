package com.ravendarque.deaftly.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatTextSizeTest {
    @Test
    fun theThreeSizesAre18And20And22Sp() {
        assertEquals(listOf(18, 20, 22), ChatTextSize.entries.map { it.sp })
    }

    @Test
    fun mediumIsTheDefault() {
        assertEquals(ChatTextSize.Medium, ChatTextSize.Default)
    }

    @Test
    fun cyclesThroughEverySizeAndBack() {
        assertEquals(ChatTextSize.Medium, ChatTextSize.Small.next())
        assertEquals(ChatTextSize.Large, ChatTextSize.Medium.next())
        assertEquals(ChatTextSize.Small, ChatTextSize.Large.next())
    }
}
