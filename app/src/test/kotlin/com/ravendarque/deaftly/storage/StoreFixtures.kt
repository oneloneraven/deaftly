package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template

val greeting =
    Template(
        id = "builtin-greeting",
        title = "Say hello",
        category = "Everyday",
        body = "Hi! I'm {name}. Please {how to reply}.",
        pinned = true,
        builtIn = true,
        placeholders =
            mapOf(
                "name" to Placeholder("name", "Your name", PlaceholderType.Typing),
                "how to reply" to
                    Placeholder(
                        key = "how to reply",
                        label = "How should they reply?",
                        type = PlaceholderType.Choices,
                        options = listOf("type on my phone", "write it down"),
                        repeatable = true,
                    ),
            ),
    )

val menu =
    Template(
        id = "builtin-menu",
        title = "Order",
        category = "Food",
        body = "Could I have {item}?",
        builtIn = true,
        placeholders = mapOf("item" to Placeholder("item", "Item", PlaceholderType.Camera)),
    )

val builtIns = listOf(greeting, menu)
