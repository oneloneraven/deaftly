package com.ravendarque.deaftly.ui.home

import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template

private val howToReply = Placeholder("how to reply", "how to reply", PlaceholderType.Choices)

val homeTemplates =
    listOf(
        Template(
            id = "check-in",
            title = "Check in",
            category = "Everyday",
            body = "Hi! I'm deaf. I'd like to check in. My name is {name}.",
            pinned = true,
            placeholders = mapOf("name" to Placeholder("name", "name", PlaceholderType.Typing)),
        ),
        Template(
            id = "order-food",
            title = "Order food",
            category = "Food & drink",
            body = "Could I have {menu item} please?",
            pinned = true,
            placeholders = mapOf("menu item" to Placeholder("menu item", "menu item", PlaceholderType.Camera)),
        ),
        Template(
            id = "explain-deaf",
            title = "Explain I'm deaf",
            category = "Everyday",
            body = "I'm deaf. I can't hear you. Please {how to reply}",
            placeholders = mapOf("how to reply" to howToReply),
        ),
        Template(
            id = "ask-help",
            title = "Ask for help",
            category = "Everyday",
            body = "Excuse me. I'm deaf. Can you help me find {thing}?",
            placeholders = mapOf("thing" to Placeholder("thing", "thing", PlaceholderType.Choices)),
        ),
        Template(
            id = "doctor",
            title = "At the doctor's",
            category = "Health",
            body = "I'm deaf. Please write down what the doctor says.",
        ),
    )
