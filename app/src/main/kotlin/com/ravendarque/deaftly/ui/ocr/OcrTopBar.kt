package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.CameraColours
import com.ravendarque.deaftly.ui.theme.Deaftly

private val ChromeButtonSize = 48.dp
private val ChromeIconSize = 22.dp
private val PillMinHeight = 40.dp
private val PillDot = 8.dp
private val ChromeScrim = CameraColours.scrim

@Composable
fun OcrTopBar(
    status: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ChromeButton(
            icon = DeaftlyIcons.Close,
            label = stringResource(R.string.ocr_close),
            onClick = onClose,
        )
        StatusPill(status)
        Row(horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
            ChromeButton(
                icon = DeaftlyIcons.Torch,
                label = stringResource(R.string.ocr_torch),
                onClick = {},
                enabled = controlsEnabled,
            )
            ChromeButton(
                icon = DeaftlyIcons.Pause,
                label = stringResource(R.string.ocr_freeze),
                onClick = {},
                enabled = controlsEnabled,
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Row(
        modifier =
            Modifier
                .heightIn(min = PillMinHeight)
                .clip(CircleShape)
                .background(ChromeScrim)
                .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        Box(Modifier.size(PillDot).background(Deaftly.colors.highlight, CircleShape))
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun ChromeButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .size(ChromeButtonSize)
                .clip(CircleShape)
                .background(ChromeScrim)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ChromeIconSize),
            tint = if (enabled) Color.White else Color.White.copy(alpha = DISABLED_ALPHA),
        )
    }
}

private const val DISABLED_ALPHA = 0.5f
