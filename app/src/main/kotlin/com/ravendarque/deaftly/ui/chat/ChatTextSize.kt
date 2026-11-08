package com.ravendarque.deaftly.ui.chat

import androidx.annotation.StringRes
import com.ravendarque.deaftly.R

private const val NORMAL_SP = 19
private const val LARGE_SP = 26
private const val LARGEST_SP = 34

enum class ChatTextSize(
    val sp: Int,
    @StringRes val label: Int,
) {
    Normal(NORMAL_SP, R.string.chat_text_size_normal),
    Large(LARGE_SP, R.string.chat_text_size_large),
    Largest(LARGEST_SP, R.string.chat_text_size_largest),
    ;

    fun next(): ChatTextSize = entries[(ordinal + 1) % entries.size]
}
