package com.oneloneraven.deaftly.ui.show

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.oneloneraven.deaftly.navigation.HighlightRange
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowHighlightTest {
    @Test
    fun emboldensExactlyTheGivenRangesWithoutColouringThem() {
        val styled = emphasised("Hi Sam, hi.", listOf(HighlightRange(3, 6)))

        assertEquals("Hi Sam, hi.", styled.text)
        assertEquals(listOf(3 to 6), styled.spanStyles.map { it.start to it.end })
        val style = styled.spanStyles.single().item
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(Color.Unspecified, style.background)
        assertEquals(Color.Unspecified, style.color)
    }

    @Test
    fun noRangesMeansNoStyling() {
        assertEquals(emptyList(), emphasised("Plain text", emptyList()).spanStyles)
    }

    @Test
    fun rangesOutsideTheTextAreClampedOrDropped() {
        val ranges = listOf(HighlightRange(2, 99), HighlightRange(50, 60), HighlightRange(-4, 0))
        val styled = emphasised("Short", ranges)

        assertEquals(listOf(2 to 5), styled.spanStyles.map { it.start to it.end })
    }
}
