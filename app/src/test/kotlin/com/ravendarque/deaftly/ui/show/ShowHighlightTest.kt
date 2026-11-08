package com.ravendarque.deaftly.ui.show

import androidx.compose.ui.graphics.Color
import com.ravendarque.deaftly.navigation.HighlightRange
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowHighlightTest {
    private val background = Color(0xFFFFE15C)
    private val foreground = Color.Black

    @Test
    fun highlightsExactlyTheGivenRanges() {
        val styled = highlighted("Hi Sam, hi.", listOf(HighlightRange(3, 6)), background, foreground)

        assertEquals("Hi Sam, hi.", styled.text)
        assertEquals(listOf(3 to 6), styled.spanStyles.map { it.start to it.end })
        assertEquals(
            background,
            styled.spanStyles
                .single()
                .item.background,
        )
        assertEquals(
            foreground,
            styled.spanStyles
                .single()
                .item.color,
        )
    }

    @Test
    fun noRangesMeansNoStyling() {
        assertEquals(emptyList(), highlighted("Plain text", emptyList(), background, foreground).spanStyles)
    }

    @Test
    fun rangesOutsideTheTextAreClampedOrDropped() {
        val ranges = listOf(HighlightRange(2, 99), HighlightRange(50, 60), HighlightRange(-4, 0))
        val styled = highlighted("Short", ranges, background, foreground)

        assertEquals(listOf(2 to 5), styled.spanStyles.map { it.start to it.end })
    }
}
