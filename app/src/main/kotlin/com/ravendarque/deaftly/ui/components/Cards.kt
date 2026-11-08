package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.ui.theme.Deaftly

sealed interface PreviewPart {
    data class Plain(
        val text: String,
    ) : PreviewPart

    data class Placeholder(
        val label: String,
    ) : PreviewPart
}

enum class TileTone { Accent, PersonB }

private const val NO_BREAK_SPACE = " "
private val TileMinHeight = 132.dp
private val TileIconSize = 26.dp
private val CardMinHeight = 48.dp

@Composable
fun ListCard(
    title: String,
    preview: List<PreviewPart>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    customActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val shape = Deaftly.shapes.cardSmall
    val previewText = rememberPreview(preview)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = CardMinHeight)
                .clip(shape)
                .background(Deaftly.colors.surface, shape)
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ).semantics { this.customActions = customActions }
                .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space14),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Deaftly.colors.ink)
        Text(text = previewText, style = MaterialTheme.typography.bodySmall, color = Deaftly.colors.inkSecondary)
    }
}

@Composable
private fun rememberPreview(parts: List<PreviewPart>): AnnotatedString {
    val chipStyle =
        SpanStyle(
            color = Deaftly.colors.accentStrong,
            background = Deaftly.colors.accentSoft,
            fontWeight = FontWeight.Bold,
        )
    return remember(parts, chipStyle) {
        buildAnnotatedString {
            parts.forEach { part ->
                when (part) {
                    is PreviewPart.Plain -> {
                        append(part.text)
                    }

                    is PreviewPart.Placeholder -> {
                        withStyle(chipStyle) { append(NO_BREAK_SPACE + part.label + NO_BREAK_SPACE) }
                    }
                }
            }
        }
    }
}

@Composable
fun PinnedTile(
    title: String,
    preview: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: TileTone = TileTone.Accent,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    customActions: List<CustomAccessibilityAction> = emptyList(),
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.card
    val (container, titleColour, previewColour) =
        when (tone) {
            TileTone.Accent -> Triple(c.accent, c.onAccent, c.onAccentMuted)
            TileTone.PersonB -> Triple(c.personB, c.onPersonB, c.onPersonBMuted)
        }
    Column(
        modifier =
            modifier
                .heightIn(min = TileMinHeight)
                .clip(shape)
                .background(container, shape)
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                ).semantics { this.customActions = customActions }
                .padding(Deaftly.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(TileIconSize), tint = titleColour)
        Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = titleColour)
            Text(text = preview, style = MaterialTheme.typography.bodySmall, color = previewColour, maxLines = 2)
        }
    }
}
