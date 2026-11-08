package com.ravendarque.deaftly.ui.ocr

class FakeLine(
    val text: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

val fakeMenu =
    listOf(
        FakeLine("PASTA", 0.35f, 0.175f, 0.30f, 0.035f),
        FakeLine("Pasta arrabbiata   12.50", 0.12f, 0.235f, 0.76f, 0.035f),
        FakeLine("tomato, garlic, chilli", 0.12f, 0.285f, 0.50f, 0.028f),
        FakeLine("Spaghetti carbonara   13.00", 0.12f, 0.335f, 0.76f, 0.035f),
        FakeLine("Penne al pesto   11.50", 0.12f, 0.385f, 0.76f, 0.035f),
        FakeLine("Lasagne   14.00", 0.12f, 0.435f, 0.76f, 0.035f),
        FakeLine("SIDES", 0.38f, 0.495f, 0.24f, 0.035f),
        FakeLine("Garlic bread   4.50", 0.12f, 0.545f, 0.76f, 0.035f),
    )
