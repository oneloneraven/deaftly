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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.CameraColours
import com.ravendarque.deaftly.ui.theme.Deaftly

private val ChromeButtonSize = 48.dp
private val ChromeIconSize = 22.dp
private val PillMinHeight = 40.dp
private val PillDot = 8.dp
private const val DISABLED_ALPHA = 0.5f

data class ToggleControl(
    val on: Boolean,
    val enabled: Boolean,
    val onToggle: () -> Unit,
)

@Composable
fun OcrTopBar(
    status: String,
    onClose: () -> Unit,
    freeze: ToggleControl,
    torch: ToggleControl?,
    modifier: Modifier = Modifier,
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
            if (torch != null) {
                ChromeToggle(
                    control = torch,
                    icon = DeaftlyIcons.Torch,
                    label = stringResource(R.string.ocr_torch),
                    state = stringResource(if (torch.on) R.string.ocr_torch_on else R.string.ocr_torch_off),
                    onColours = Deaftly.colors.highlight to Deaftly.colors.onHighlight,
                )
            }
            ChromeToggle(
                control = freeze,
                icon = if (freeze.on) DeaftlyIcons.Play else DeaftlyIcons.Pause,
                label = stringResource(R.string.ocr_freeze),
                state = stringResource(if (freeze.on) R.string.ocr_status_frozen else R.string.ocr_status_live),
                onColours = Color.White to CameraColours.onLight,
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
                .background(CameraColours.scrim)
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
) {
    Box(
        modifier =
            Modifier
                .size(ChromeButtonSize)
                .clip(CircleShape)
                .background(CameraColours.scrim)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ChromeIconSize), tint = Color.White)
    }
}

@Composable
private fun ChromeToggle(
    control: ToggleControl,
    icon: ImageVector,
    label: String,
    state: String,
    onColours: Pair<Color, Color>,
) {
    val (container, content) = if (control.on) onColours else CameraColours.scrim to Color.White
    Box(
        modifier =
            Modifier
                .size(ChromeButtonSize)
                .clip(CircleShape)
                .background(container)
                .toggleable(value = control.on, enabled = control.enabled, role = Role.Switch, onValueChange = { control.onToggle() })
                .semantics {
                    contentDescription = label
                    stateDescription = state
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ChromeIconSize),
            tint = if (control.enabled) content else content.copy(alpha = DISABLED_ALPHA),
        )
    }
}
