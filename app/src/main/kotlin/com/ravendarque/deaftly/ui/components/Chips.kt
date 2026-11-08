package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.ui.theme.Deaftly

private val ChipTarget = 48.dp
private val ChipHeight = 40.dp
private val ChipOutline = 1.5.dp

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deaftly.colors
    val shape = Deaftly.shapes.pill
    Box(
        modifier =
            modifier
                .heightIn(min = ChipTarget)
                .widthIn(min = ChipTarget)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .heightIn(min = ChipHeight)
                    .clip(shape)
                    .then(
                        if (selected) {
                            Modifier.background(c.ink, shape)
                        } else {
                            Modifier.border(ChipOutline, c.outline, shape)
                        },
                    ).padding(horizontal = Deaftly.spacing.space16),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                color = if (selected) c.ground else c.ink,
            )
        }
    }
}
