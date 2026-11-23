package com.ravendarque.deaftly.core.settings

private const val SMALL_SP = 36
private const val MEDIUM_SP = 46
private const val LARGE_SP = 56

public enum class ShowTextSize(
    public val sp: Int,
) {
    Small(SMALL_SP),
    Medium(MEDIUM_SP),
    Large(LARGE_SP),
    ;

    public fun larger(): ShowTextSize = entries.getOrElse(ordinal + 1) { this }

    public fun smaller(): ShowTextSize = entries.getOrElse(ordinal - 1) { this }

    public companion object {
        public val Default: ShowTextSize = Medium
    }
}
