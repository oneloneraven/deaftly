package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PaperColour = Color(0xFFF3ECDC)
private val InkColour = Color(0xFF2A2018)
private const val MONOSPACE_ADVANCE = 0.6f

@Composable
fun FakeCamera(
    modifier: Modifier = Modifier,
    lines: List<FakeLine> = fakeMenu,
) {
    BoxWithConstraints(
        modifier =
            modifier.background(
                Brush.radialGradient(listOf(Color(0xFF4A3F33), Color(0xFF241E19), Color(0xFF120F0D))),
            ),
    ) {
        val density = LocalDensity.current
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        Box(
            Modifier
                .offset(screenWidth * PAPER_LEFT, screenHeight * PAPER_TOP)
                .size(screenWidth * PAPER_WIDTH, screenHeight * PAPER_HEIGHT)
                .clip(RoundedCornerShape(4.dp))
                .background(PaperColour),
        )
        lines.forEach { line ->
            val lineWidth = screenWidth * line.width
            val charWidth = lineWidth / line.text.length
            val fontSize = with(density) { minOf(charWidth / MONOSPACE_ADVANCE, screenHeight * line.height).toSp() }
            Box(
                Modifier
                    .offset(screenWidth * line.left, screenHeight * line.top)
                    .size(lineWidth, screenHeight * line.height),
            ) {
                Text(
                    text = line.text,
                    style =
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            color = InkColour,
                        ),
                    softWrap = false,
                )
            }
        }
    }
}

private const val PAPER_LEFT = 0.08f
private const val PAPER_TOP = 0.14f
private const val PAPER_WIDTH = 0.84f
private const val PAPER_HEIGHT = 0.56f
