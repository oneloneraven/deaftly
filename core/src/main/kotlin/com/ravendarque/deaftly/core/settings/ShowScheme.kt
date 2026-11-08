package com.ravendarque.deaftly.core.settings

public enum class ShowScheme {
    BlackOnWhite,
    WhiteOnBlack,
    YellowOnBlack,
    ;

    public fun next(): ShowScheme = entries[(ordinal + 1) % entries.size]
}
