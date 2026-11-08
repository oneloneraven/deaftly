package com.ravendarque.deaftly.core.settings

public enum class ChatLayout {
    Sides,
    Split,
    ;

    public fun other(): ChatLayout = if (this == Sides) Split else Sides
}
