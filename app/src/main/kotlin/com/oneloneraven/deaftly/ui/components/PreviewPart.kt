package com.oneloneraven.deaftly.ui.components

sealed interface PreviewPart {
    data class Plain(
        val text: String,
    ) : PreviewPart

    data class Placeholder(
        val label: String,
    ) : PreviewPart
}
