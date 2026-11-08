package com.ravendarque.deaftly.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f
private const val STROKE_WIDTH = 2f

private fun strokeIcon(
    name: String,
    vararg paths: String,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = VIEWPORT.dp,
            defaultHeight = VIEWPORT.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        ).apply {
            paths.forEach { data ->
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = STROKE_WIDTH,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

object DeaftlyIcons {
    val Back: ImageVector by lazy { strokeIcon("Back", "M15 18l-6-6 6-6") }

    val Close: ImageVector by lazy { strokeIcon("Close", "M6 6l12 12M18 6L6 18") }

    val Plus: ImageVector by lazy { strokeIcon("Plus", "M12 5v14M5 12h14") }

    val Edit: ImageVector by lazy { strokeIcon("Edit", "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z") }

    val Scan: ImageVector by lazy {
        strokeIcon(
            "Scan",
            "M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3",
            "M7 9h10M7 12h7M7 15h9",
        )
    }

    val Chat: ImageVector by lazy { strokeIcon("Chat", "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z") }

    val Search: ImageVector by lazy {
        strokeIcon("Search", "M18 11a7 7 0 1 0-14 0 7 7 0 1 0 14 0", "M20 20l-3.5-3.5")
    }

    val Settings: ImageVector by lazy {
        strokeIcon(
            "Settings",
            "M15 12a3 3 0 1 0-6 0 3 3 0 1 0 6 0",
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21" +
                "a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1" +
                "a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 " +
                "1.7 1.7 0 0 0-.3-1.8" +
                "l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1" +
                "a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9" +
                "a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        )
    }

    val Contrast: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "Contrast",
                defaultWidth = VIEWPORT.dp,
                defaultHeight = VIEWPORT.dp,
                viewportWidth = VIEWPORT,
                viewportHeight = VIEWPORT,
            ).apply {
                addPath(
                    pathData = PathParser().parsePathString("M21 12a9 9 0 1 1-18 0 9 9 0 1 1 18 0").toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = STROKE_WIDTH,
                )
                addPath(
                    pathData = PathParser().parsePathString("M12 3a9 9 0 0 1 0 18z").toNodes(),
                    fill = SolidColor(Color.Black),
                )
            }.build()
    }

    val Check: ImageVector by lazy { strokeIcon("Check", "M5 12l5 5L20 7") }

    val Expand: ImageVector by lazy { strokeIcon("Expand", "M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7") }

    val Torch: ImageVector by lazy { strokeIcon("Torch", "M13 2L4 14h7l-1 8 9-12h-7z") }

    val Send: ImageVector by lazy { strokeIcon("Send", "M22 2L11 13M22 2l-7 20-4-9-9-4z") }

    val Type: ImageVector by lazy { strokeIcon("Type", "M4 7V4h16v3M9 20h6M12 4v16") }

    val Home: ImageVector by lazy { strokeIcon("Home", "M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6") }

    val Utensils: ImageVector by lazy {
        strokeIcon("Utensils", "M7 3v8a2 2 0 0 0 2 2v8M11 3v8M7 7h4M17 3c-2 0-3 3-3 6s1 4 3 4v8")
    }

    val Trash: ImageVector by lazy { strokeIcon("Trash", "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14") }
}
