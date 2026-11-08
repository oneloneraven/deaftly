package com.ravendarque.deaftly.ui.ocr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

sealed interface OcrOutcome {
    val templateId: String
    val placeholderKey: String

    data class Text(
        override val templateId: String,
        override val placeholderKey: String,
        val text: String,
    ) : OcrOutcome

    data class TypeInstead(
        override val templateId: String,
        override val placeholderKey: String,
    ) : OcrOutcome
}

class OcrOutcomes {
    private val queue = MutableStateFlow<List<OcrOutcome>>(emptyList())

    val pending: StateFlow<List<OcrOutcome>> get() = queue

    fun post(outcome: OcrOutcome) {
        queue.update { it + outcome }
    }

    fun consume(outcome: OcrOutcome) {
        queue.update { it - outcome }
    }
}
