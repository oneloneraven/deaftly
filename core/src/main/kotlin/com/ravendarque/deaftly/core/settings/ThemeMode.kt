package com.ravendarque.deaftly.core.settings

public enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    public fun isDark(systemIsDark: Boolean): Boolean =
        when (this) {
            System -> systemIsDark
            Light -> false
            Dark -> true
        }
}
