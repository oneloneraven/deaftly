package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun hintedFieldDecorator(
    isEmpty: Boolean,
    hint: String,
    textStyle: TextStyle,
    hintColor: Color,
    minHeight: Dp,
): TextFieldDecorator {
    val density = LocalDensity.current
    val inset = with(density) { ((minHeight - textStyle.lineHeight.toDp()) / 2).coerceAtLeast(0.dp) }
    return remember(isEmpty, hint, textStyle, hintColor, minHeight, inset) {
        TextFieldDecorator { inner ->
            Box(Modifier.heightIn(min = minHeight).padding(vertical = inset), contentAlignment = Alignment.TopStart) {
                if (isEmpty) Text(hint, style = textStyle, color = hintColor)
                inner()
            }
        }
    }
}
