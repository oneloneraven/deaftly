package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

enum class ChipState { Empty, Filled, Focused }

enum class ChipTone { Accent, PersonB }

private val MinTarget = 48.dp
private val DashedStroke = 2.dp
private val SolidStroke = 2.dp
private val Halo = 4.dp
private val HaloRadius = 8.dp
private val ChipPadding = 12.dp
private val ChipIconGap = 4.dp
private val SmallIcon = 16.dp
private const val DASH_ON = 10f
private const val DASH_OFF = 7f

private class ChipStyle(
    val container: Color,
    val content: Color,
    val border: Color,
    val dashed: Boolean,
    val ring: Color?,
)

@Composable
private fun chipStyle(
    state: ChipState,
    tone: ChipTone,
    focusedByInput: Boolean,
): ChipStyle {
    val c = Deaftly.colors
    val showRing = state == ChipState.Focused || focusedByInput
    return when (tone) {
        ChipTone.Accent -> {
            when {
                showRing -> ChipStyle(c.surface, c.accentStrong, c.accent, dashed = false, ring = c.accentSoft)
                state == ChipState.Filled -> ChipStyle(c.accentSoft, c.accentStrong, Color.Transparent, false, null)
                else -> ChipStyle(c.surface, c.accentStrong, c.accent, dashed = true, ring = null)
            }
        }

        ChipTone.PersonB -> {
            when {
                showRing -> ChipStyle(c.personB, c.onPersonB, Color.Transparent, dashed = false, ring = c.ink)
                state == ChipState.Filled -> ChipStyle(c.personB, c.onPersonB, Color.Transparent, false, null)
                else -> ChipStyle(c.surface, c.onPersonBMuted, c.personBOutline, dashed = true, ring = null)
            }
        }
    }
}

@Composable
private fun ChipFrame(
    style: ChipStyle,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = Deaftly.shapes.chip
    val haloPx = with(LocalDensity.current) { Halo.toPx() }
    val strokePx = with(LocalDensity.current) { DashedStroke.toPx() }
    Row(
        modifier =
            modifier
                .drawBehind {
                    style.ring?.let { ring ->
                        drawRoundRect(
                            color = ring,
                            topLeft = Offset(-haloPx, -haloPx),
                            size = Size(size.width + 2 * haloPx, size.height + 2 * haloPx),
                            cornerRadius =
                                CornerRadius(HaloRadius.toPx() + haloPx),
                        )
                    }
                }.background(style.container, shape)
                .then(
                    if (style.dashed) {
                        Modifier.drawBehind {
                            drawOutline(
                                outline = shape.createOutline(size, layoutDirection, this),
                                color = style.border,
                                style =
                                    Stroke(
                                        width = strokePx,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)),
                                    ),
                            )
                        }
                    } else if (style.border != Color.Transparent) {
                        Modifier.border(SolidStroke, style.border, shape)
                    } else {
                        Modifier
                    },
                ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun stateText(state: ChipState): String =
    stringResource(
        when (state) {
            ChipState.Empty -> R.string.chip_state_empty
            ChipState.Filled -> R.string.chip_state_filled
            ChipState.Focused -> R.string.chip_state_focused
        },
    )

@Composable
fun PlaceholderChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ChipState = ChipState.Filled,
    tone: ChipTone = ChipTone.Accent,
    onRemove: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focusedByInput by interaction.collectIsFocusedAsState()
    val style = chipStyle(state, tone, focusedByInput)
    val removable = onRemove != null && state != ChipState.Empty
    val stateDescription = stateText(state)
    ChipFrame(style, modifier) {
        Box(
            modifier =
                Modifier
                    .heightIn(min = MinTarget)
                    .widthIn(min = MinTarget)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    ).semantics { this.stateDescription = stateDescription }
                    .padding(start = ChipPadding, end = if (removable) ChipIconGap else ChipPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = style.content)
        }
        if (removable) {
            val removeLabel = stringResource(R.string.chip_remove, label)
            Box(
                modifier =
                    Modifier
                        .size(MinTarget)
                        .clickable(role = Role.Button, onClick = requireNotNull(onRemove))
                        .semantics { contentDescription = removeLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = DeaftlyIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SmallIcon),
                    tint = style.content,
                )
            }
        }
    }
}

@Composable
fun AddMoreChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.Accent,
) {
    val style = chipStyle(ChipState.Empty, tone, focusedByInput = false)
    ChipFrame(style, modifier) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = MinTarget)
                    .widthIn(min = MinTarget)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = ChipPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = DeaftlyIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(SmallIcon),
                tint = style.content,
            )
            Spacer(Modifier.width(ChipIconGap))
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = style.content)
        }
    }
}
