package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.ui.theme.Deaftly

private val QuoteBar = 3.dp

@Composable
fun QuoteBlock(
    text: String,
    bar: Color,
    colour: Color,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(QuoteBar).fillMaxHeight().background(bar))
        Text(
            text = text,
            color = colour,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = Deaftly.spacing.space10),
        )
    }
}
