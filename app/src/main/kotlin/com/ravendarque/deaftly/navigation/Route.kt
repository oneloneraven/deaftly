package com.ravendarque.deaftly.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object Templates : Route

    @Serializable
    data class Fill(
        val templateId: String,
        val forChat: Boolean = false,
    ) : Route

    @Serializable
    data object TemplatePicker : Route

    @Serializable
    data class Show(
        val text: String = "",
        val highlights: List<HighlightRange> = emptyList(),
    ) : Route

    @Serializable
    data object TypeShow : Route

    @Serializable
    data class Chat(
        val messages: List<String> = emptyList(),
        val draft: String = "",
    ) : Route

    @Serializable
    data object FaceToFace : Route

    @Serializable
    data class Ocr(
        val templateId: String,
        val placeholderKey: String,
    ) : Route

    @Serializable
    data class Editor(
        val templateId: String? = null,
    ) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Help : Route

    @Serializable
    data class HelpPage(
        val slug: String,
    ) : Route

    @Serializable
    data object Share : Route

    @Serializable
    data object FirstRun : Route
}

@Serializable
data class HighlightRange(
    val start: Int,
    val end: Int,
)
