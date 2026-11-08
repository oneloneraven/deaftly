package com.ravendarque.deaftly.ui.fill

import com.ravendarque.deaftly.core.template.TemplateFill
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal object FillCodec {
    private val serializer = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

    fun encode(
        keys: Set<String>,
        fill: TemplateFill,
    ): String = Json.encodeToString(serializer, keys.associateWith(fill::values).filterValues { it.isNotEmpty() })

    fun decode(encoded: String?): TemplateFill =
        if (encoded == null) {
            TemplateFill()
        } else {
            Json.decodeFromString(serializer, encoded).entries.fold(TemplateFill()) { fill, (key, values) ->
                values.fold(fill) { acc, value -> acc.add(key, value) }
            }
        }
}
