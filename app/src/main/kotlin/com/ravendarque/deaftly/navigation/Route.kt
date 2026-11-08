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
    ) : Route

    @Serializable
    data object Show : Route

    @Serializable
    data object Chat : Route

    @Serializable
    data object Ocr : Route

    @Serializable
    data object Editor : Route

    @Serializable
    data object Settings : Route
}

@StringRes
fun Route.titleRes(): Int =
    when (this) {
        Route.Home -> R.string.app_name
        is Route.Fill -> R.string.route_fill
        Route.Show -> R.string.route_show
        Route.Chat -> R.string.route_chat
        Route.Ocr -> R.string.route_ocr
        Route.Editor -> R.string.route_editor
        Route.Settings -> R.string.route_settings
    }
