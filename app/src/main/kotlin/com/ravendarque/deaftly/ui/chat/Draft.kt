package com.ravendarque.deaftly.ui.chat

import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Quote

data class Draft(
    val composition: Composition = Composition(),
    val quote: Quote? = null,
) {
    val text: String get() = composition.text
}
