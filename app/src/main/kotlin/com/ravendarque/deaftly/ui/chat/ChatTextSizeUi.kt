package com.ravendarque.deaftly.ui.chat

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.settings.ChatTextSize

private const val SMALL_GLYPH_SP = 15
private const val MEDIUM_GLYPH_SP = 20
private const val LARGE_GLYPH_SP = 26
private const val LINE_HEIGHT_RATIO = 1.4f

@get:StringRes
val ChatTextSize.label: Int
    get() =
        when (this) {
            ChatTextSize.Small -> R.string.chat_text_size_small
            ChatTextSize.Medium -> R.string.chat_text_size_medium
            ChatTextSize.Large -> R.string.chat_text_size_large
        }

val ChatTextSize.glyphSp: Int
    get() =
        when (this) {
            ChatTextSize.Small -> SMALL_GLYPH_SP
            ChatTextSize.Medium -> MEDIUM_GLYPH_SP
            ChatTextSize.Large -> LARGE_GLYPH_SP
        }

@Composable
fun chatTextStyle(sp: Int): TextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = sp.sp, lineHeight = (sp * LINE_HEIGHT_RATIO).sp)
