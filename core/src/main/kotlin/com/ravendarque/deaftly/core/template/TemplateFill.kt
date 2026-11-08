package com.ravendarque.deaftly.core.template

public class TemplateFill private constructor(
    private val entries: Map<String, List<String>>,
) {
    public constructor() : this(emptyMap())

    public fun values(key: String): List<String> = entries[key].orEmpty()

    public fun value(key: String): String? = entries[key]?.firstOrNull()

    public fun set(
        key: String,
        value: String,
    ): TemplateFill = TemplateFill(entries + (key to listOf(value)))

    public fun add(
        key: String,
        value: String,
    ): TemplateFill = TemplateFill(entries + (key to values(key) + value))

    public fun removeAt(
        key: String,
        index: Int,
    ): TemplateFill {
        val current = values(key)
        if (index !in current.indices) throw IndexOutOfBoundsException("No value $index for placeholder")
        val remaining = current.filterIndexed { i, _ -> i != index }
        return if (remaining.isEmpty()) clear(key) else TemplateFill(entries + (key to remaining))
    }

    public fun clear(key: String): TemplateFill = if (key in entries) TemplateFill(entries - key) else this

    override fun equals(other: Any?): Boolean = other is TemplateFill && entries == other.entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "TemplateFill(keys=${entries.keys})"
}
