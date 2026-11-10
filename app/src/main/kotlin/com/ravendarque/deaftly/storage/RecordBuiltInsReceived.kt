package com.ravendarque.deaftly.storage

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val storeMigrations: List<StoreMigration> = listOf(RecordBuiltInsReceived)

object RecordBuiltInsReceived : StoreMigration {
    override val from = 1

    val shippedAtVersion1: Map<String, String> =
        mapOf(
            "builtin.check-in" to "80fa68ecc1e0caadf134674c291ed8a77ce0fb96cd29f313c7fdaab4671696be",
            "builtin.order-food" to "47b0602faabfc7b44d31513b858418692cd8f7c07a460d677c84d3a6a6ed377f",
            "builtin.explain-deaf" to "e48b02a537fedb5c02e19597ca5448f98aba06a5259fdb458706c811cbe74e3d",
            "builtin.ask-help" to "4368634558bd3b0ec813c0d0834cb600e915dd3f606ec9ba2d38484ac6c6d57a",
            "builtin.food-allergy" to "68960d5184c1df4d51af8c575bcbbaa18acb75656a4102826f817f551082b2aa",
            "builtin.doctor" to "8c2b2832937c7d5b342201445da76342a24506a71434ceb9ff54774f55b71e64",
            "builtin.pharmacy" to "ebe5f7f952363a147d1385f3e399b5d77143a226877d2897ab5acc40ba3cfccb",
            "builtin.buy-ticket" to "90c16a02a716e877c608296b1bcd9327d3ae01d2fd6d886206b492267b595afc",
            "builtin.paying" to "2fa3279e678b5f76c64861580bbc34d2e0203257a5575bef7e12fa8897ee5daf",
        )

    override fun migrate(document: JsonObject): JsonObject =
        JsonObject(document + ("builtInsReceived" to JsonObject(shippedAtVersion1.mapValues { JsonPrimitive(it.value) })))
}
