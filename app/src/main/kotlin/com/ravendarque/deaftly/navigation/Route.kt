package com.ravendarque.deaftly.navigation

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.ravendarque.deaftly.R
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

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
        val firstMessage: String? = null,
    ) : Route

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
    data object FirstRun : Route
}

@Serializable
data class HighlightRange(
    val start: Int,
    val end: Int,
)

@StringRes
fun Route.titleRes(): Int =
    when (this) {
        Route.Home -> R.string.app_name
        is Route.Fill -> R.string.route_fill
        Route.TemplatePicker -> R.string.chat_insert_template
        is Route.Show -> R.string.route_show
        Route.TypeShow -> R.string.home_type_and_show
        is Route.Chat -> R.string.route_chat
        is Route.Ocr -> R.string.route_ocr
        is Route.Editor -> R.string.route_editor
        Route.Settings -> R.string.route_settings
        Route.FirstRun -> R.string.first_run_title
    }
