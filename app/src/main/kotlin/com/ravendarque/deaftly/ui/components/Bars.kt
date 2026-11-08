package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private val BarTarget = 48.dp
private const val DOCK_GRADIENT_SOLID_FROM = 0.3f
private const val DOCK_STACK_FONT_SCALE = 1.5f

@Composable
fun DeaftlyTopBar(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector = DeaftlyIcons.Back,
    navigationLabel: String = stringResource(R.string.back),
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeaftlyIconButton(navigationIcon, navigationLabel, onNavigate)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Deaftly.colors.ink,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = Deaftly.spacing.space8)
                    .semantics { heading() },
        )
        if (trailing != null) {
            Row(
                modifier = Modifier.heightIn(min = BarTarget),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        } else {
            Box(Modifier.size(BarTarget))
        }
    }
}

@Composable
fun ActionDock(
    start: @Composable (Modifier) -> Unit,
    end: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ground = Deaftly.colors.ground
    val stacked = LocalDensity.current.fontScale >= DOCK_STACK_FONT_SCALE
    val frame =
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to ground.copy(alpha = 0f),
                    DOCK_GRADIENT_SOLID_FROM to ground,
                    1f to ground,
                ),
            ).padding(
                start = Deaftly.spacing.space20,
                top = Deaftly.spacing.space14,
                end = Deaftly.spacing.space20,
                bottom = Deaftly.spacing.space24,
            )
    if (stacked) {
        Column(modifier = frame, verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12)) {
            start(Modifier.fillMaxWidth())
            end(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = frame.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
        ) {
            Box(Modifier.weight(1f)) { start(Modifier.fillMaxSize()) }
            Box(Modifier.weight(1f)) { end(Modifier.fillMaxSize()) }
        }
    }
}
