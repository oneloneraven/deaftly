package com.ravendarque.deaftly.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val AppName = Regex("deaftly", RegexOption.IGNORE_CASE)

internal fun spokenText(text: String): String = AppName.replace(text) { if (it.value.first().isUpperCase()) "Deftly" else "deftly" }

fun Modifier.spoken(text: String): Modifier {
    val spoken = spokenText(text)
    return if (spoken == text) this else semantics { contentDescription = spoken }
}
