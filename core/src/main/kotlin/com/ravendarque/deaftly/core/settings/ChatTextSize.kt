package com.ravendarque.deaftly.core.settings

private const val SMALL_SP = 18
private const val MEDIUM_SP = 20
private const val LARGE_SP = 22

public enum class ChatTextSize(
    public val sp: Int,
) {
    Small(SMALL_SP),
    Medium(MEDIUM_SP),
    Large(LARGE_SP),
    ;

    public fun next(): ChatTextSize = entries[(ordinal + 1) % entries.size]

    public companion object {
        public val Default: ChatTextSize = Medium
    }
}
