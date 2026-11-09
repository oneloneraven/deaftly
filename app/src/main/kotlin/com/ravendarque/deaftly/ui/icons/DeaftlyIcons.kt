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

private fun fillIcon(
    name: String,
    path: String,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = VIEWPORT.dp,
            defaultHeight = VIEWPORT.dp,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT,
        ).apply {
            addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        }.build()

private fun rectPath(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    radius: Int,
): String {
    val straightWidth = width - 2 * radius
    val straightHeight = height - 2 * radius
    val corner = "a$radius $radius 0 0 1"
    return "M${x + radius} ${y}h$straightWidth$corner $radius $radius" +
        "v$straightHeight$corner -$radius $radius" +
        "h-$straightWidth$corner -$radius -$radius" +
        "v-$straightHeight$corner $radius -$radius" +
        "z"
}

object DeaftlyIcons {
    val Back: ImageVector by lazy { strokeIcon("Back", "M15 18l-6-6 6-6") }

    val Chevron: ImageVector by lazy { strokeIcon("Chevron", "M9 6l6 6-6 6") }

    val Close: ImageVector by lazy { strokeIcon("Close", "M6 6l12 12M18 6L6 18") }

    val ClearCircle: ImageVector by lazy { strokeIcon("ClearCircle", "M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", "M15 9l-6 6M9 9l6 6") }

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

    val Pause: ImageVector by lazy { strokeIcon("Pause", "M8 5v14M16 5v14") }

    val Play: ImageVector by lazy { strokeIcon("Play", "M7 4l13 8-13 8z") }

    val Send: ImageVector by lazy { strokeIcon("Send", "M22 2L11 13M22 2l-7 20-4-9-9-4z") }

    val Type: ImageVector by lazy { strokeIcon("Type", "M4 7V4h16v3M9 20h6M12 4v16") }

    val Home: ImageVector by lazy { strokeIcon("Home", "M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6") }

    val Utensils: ImageVector by lazy {
        strokeIcon("Utensils", "M7 3v8a2 2 0 0 0 2 2v8M11 3v8M7 7h4M17 3c-2 0-3 3-3 6s1 4 3 4v8")
    }

    val Highlight: ImageVector by lazy {
        strokeIcon(
            "Highlight",
            "M9 11l-6 6v3h9l3-3",
            "M22 12l-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4",
            "M3 21h18",
        )
    }

    val Quote: ImageVector by lazy { fillIcon("Quote", "M4 18h5l2-4V6H4v8h3zM13 18h5l2-4V6h-7v8h3z") }

    val Template: ImageVector by lazy {
        strokeIcon("Template", "M6 4h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z", "M7 9h10M7 13h6")
    }

    val Trash: ImageVector by lazy { strokeIcon("Trash", "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14") }

    val FaceToFace: ImageVector by lazy {
        strokeIcon(
            "FaceToFace",
            rectPath(x = 6, y = 2, width = 12, height = 20, radius = 2),
            "M6 12h12M12 9V5M10 7l2-2 2 2M12 15v4M10 17l2 2 2-2",
        )
    }

    val Keyboard: ImageVector by lazy {
        strokeIcon(
            "Keyboard",
            rectPath(x = 2, y = 6, width = 20, height = 12, radius = 2),
            "M6 10h.01M10 10h.01M14 10h.01M18 10h.01M8 14h8",
        )
    }

    val TemplateBlank: ImageVector by lazy {
        strokeIcon("TemplateBlank", rectPath(x = 3, y = 4, width = 18, height = 16, radius = 2), "M7 9h10M7 14h2M11 14h2M15 14h2")
    }

    val SideBySide: ImageVector by lazy {
        strokeIcon(
            "SideBySide",
            "M4 3h9a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H8l-3 3v-3H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M18 11h2a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-1v3l-3-3h-5a2 2 0 0 1-2-2v-2",
        )
    }

    val Sides: ImageVector by lazy { strokeIcon("Sides", "M4 5h11v6H4z", "M9 13h11v6H9z") }
}
