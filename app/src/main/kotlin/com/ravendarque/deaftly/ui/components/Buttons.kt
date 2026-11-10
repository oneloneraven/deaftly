package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.ui.theme.Deaftly

internal val ButtonMinHeight = 60.dp
private val IconButtonSize = 48.dp
private val ButtonIconSize = 22.dp
private val ButtonIconGap = 8.dp
private val ButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val OutlineWidth = 1.5.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = Deaftly.shapes.button,
        contentPadding = ButtonPadding,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Deaftly.colors.accent,
                contentColor = Deaftly.colors.onAccent,
            ),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = Deaftly.shapes.button,
        contentPadding = ButtonPadding,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Deaftly.colors.ink,
                contentColor = Deaftly.colors.ground,
            ),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = Deaftly.shapes.button,
        contentPadding = ButtonPadding,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = Deaftly.colors.surface,
                contentColor = Deaftly.colors.ink,
            ),
        border = BorderStroke(OutlineWidth, Deaftly.colors.outline),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
fun DeaftlyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(IconButtonSize),
        enabled = enabled,
        shape = Deaftly.shapes.controlSmall,
        colors = IconButtonDefaults.iconButtonColors(contentColor = Deaftly.colors.ink),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
fun AccentIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(IconButtonSize),
        enabled = enabled,
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = Deaftly.colors.accent,
                contentColor = Deaftly.colors.onAccent,
            ),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    icon: ImageVector?,
) {
    if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
        Spacer(Modifier.width(ButtonIconGap))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}
