package com.oneloneraven.deaftly.ui.chat

import com.oneloneraven.deaftly.core.chat.Composition
import com.oneloneraven.deaftly.core.chat.Quote

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
