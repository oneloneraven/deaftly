package com.ravendarque.deaftly.ui.fill

import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template

val replyOptions = listOf("type on my phone", "type on your phone", "write it down", "point to the words")

val checkIn =
    Template(
        id = "check-in",
        title = "Check in",
        category = "Everyday",
        body = "Hi! I'm deaf. I'd like to check in. My name is {name}. Please {how to reply}.",
        placeholders =
            mapOf(
                "name" to Placeholder("name", "Your name", PlaceholderType.Typing),
                "how to reply" to Placeholder("how to reply", "How should they reply?", PlaceholderType.Choices, replyOptions),
            ),
    )

val orderFood =
    Template(
        id = "order-food",
        title = "Order food",
        category = "Food & drink",
        body = "Hi! I'm deaf. Could I have {menu item} please? If you have a question, please type it on my phone.",
        placeholders = mapOf("menu item" to Placeholder("menu item", "Menu item", PlaceholderType.Camera, repeatable = true)),
    )

val fillTemplates = listOf(checkIn, orderFood)
