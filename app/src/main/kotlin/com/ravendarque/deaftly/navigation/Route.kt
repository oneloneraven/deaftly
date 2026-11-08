package com.ravendarque.deaftly.navigation

import androidx.annotation.StringRes
import com.ravendarque.deaftly.R

enum class Route(
    @StringRes val title: Int,
) {
    Home(R.string.app_name),
    Fill(R.string.route_fill),
    Show(R.string.route_show),
    Chat(R.string.route_chat),
    Ocr(R.string.route_ocr),
    Editor(R.string.route_editor),
}
