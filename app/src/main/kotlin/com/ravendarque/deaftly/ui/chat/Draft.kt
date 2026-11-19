package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Quote

data class Draft(
    val composition: Composition = Composition(),
    val quote: Quote? = null,
    val editing: Editing? = null,
) {
    val text: String get() = composition.text
}

data class Editing(
    val messageId: Int,
    val resume: Draft,
)
