package com.ravendarque.deaftly.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.core.chat.SpanStyle as ChatSpanStyle

@Immutable
class ChatFormatting(
    private val highlight: Color,
    private val onHighlight: Color,
) {
    fun styleOf(style: ChatSpanStyle): SpanStyle =
        when (style) {
            ChatSpanStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
            ChatSpanStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
            ChatSpanStyle.Highlight -> SpanStyle(background = highlight, color = onHighlight, textDecoration = TextDecoration.Underline)
        }

    fun annotate(spans: List<Span>): AnnotatedString =
        buildAnnotatedString {
            spans.forEach { span ->
                val merged = span.styles.fold(SpanStyle()) { acc, style -> acc.merge(styleOf(style)) }
                withStyle(merged) { append(span.text) }
            }
        }
}

@Composable
fun rememberChatFormatting(): ChatFormatting {
    val highlight = Deaftly.colors.highlight
    val onHighlight = Deaftly.colors.onHighlight
    return remember(highlight, onHighlight) { ChatFormatting(highlight, onHighlight) }
}
