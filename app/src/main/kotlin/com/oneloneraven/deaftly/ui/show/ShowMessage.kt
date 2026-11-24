package com.oneloneraven.deaftly.ui.show

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.core.settings.ShowTextSize
import com.oneloneraven.deaftly.navigation.HighlightRange
import com.oneloneraven.deaftly.ui.icons.DeaftlyIcons
import com.oneloneraven.deaftly.ui.theme.AtkinsonHyperlegibleNext
import com.oneloneraven.deaftly.ui.theme.ShowColours
import kotlinx.coroutines.launch

private const val LARGEST_SCALE = 1.5f
private const val LINE_HEIGHT_RATIO = 1.16f
private const val FADE_SOLID_FROM = 0.5f
private val FadeHeight = 96.dp
private val MoreTarget = 48.dp

@Composable
private fun showStyle(
    color: Color,
    textSize: ShowTextSize,
): TextStyle {
    val size = with(LocalDensity.current) { minOf(textSize.sp.sp.toPx(), (textSize.sp * LARGEST_SCALE).dp.toPx()).toSp() }
    return TextStyle(
        color = color,
        fontFamily = AtkinsonHyperlegibleNext,
        fontWeight = FontWeight.Medium,
        fontSize = size,
        lineHeight = size * LINE_HEIGHT_RATIO,
        letterSpacing = (-0.01).em,
        hyphens = Hyphens.Auto,
        lineBreak = LineBreak.Paragraph,
    )
}

internal fun emphasised(
    text: String,
    highlights: List<HighlightRange>,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val mark = SpanStyle(fontWeight = FontWeight.Bold)
        highlights.forEach { range ->
            val start = range.start.coerceIn(0, text.length)
            val end = range.end.coerceIn(start, text.length)
            if (end > start) addStyle(mark, start, end)
        }
    }

@Composable
fun ShowMessage(
    text: String,
    colours: ShowColours,
    modifier: Modifier = Modifier,
    highlights: List<HighlightRange> = emptyList(),
    textSize: ShowTextSize = ShowTextSize.Default,
) {
    val styled = remember(text, highlights) { emphasised(text, highlights) }
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier) {
        val viewportHeight = maxHeight
        Box(modifier = Modifier.fillMaxWidth().verticalScroll(scroll)) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(text = styled, style = showStyle(colours.text, textSize))
            }
        }
        if (scroll.canScrollForward) {
            MoreBelow(scroll, viewportHeight, colours, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun MoreBelow(
    scroll: ScrollState,
    viewportHeight: Dp,
    colours: ShowColours,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val page = with(LocalDensity.current) { (viewportHeight - FadeHeight).coerceAtLeast(MoreTarget).toPx() }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(FadeHeight)
                .background(Brush.verticalGradient(0f to Color.Transparent, FADE_SOLID_FROM to colours.background)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = MoreTarget)
                    .background(colours.background)
                    .clickable(role = Role.Button) { scope.launch { scroll.animateScrollBy(page) } }
                    .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(DeaftlyIcons.ChevronDown, contentDescription = null, tint = colours.text, modifier = Modifier.size(28.dp))
            Text(
                text = stringResource(R.string.show_more_below),
                style = MaterialTheme.typography.titleMedium,
                color = colours.text,
            )
        }
    }
}
