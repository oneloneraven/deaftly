package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
internal fun MicButton(
    on: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    enabled: Boolean = true,
    unavailable: Boolean = false,
) {
    val c = Deaftly.colors
    val label =
        stringResource(
            when {
                unavailable -> R.string.chat_voice_unavailable_button
                on -> R.string.chat_voice_stop
                else -> R.string.chat_voice_start
            },
        )
    val stateText = stringResource(if (on) R.string.chat_format_on else R.string.chat_format_off)
    Box(
        modifier =
            Modifier
                .size(width = SendWidth, height = ControlSize)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(AccentShape)
                .background(if (unavailable) c.ground else container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = label
                    if (!unavailable) stateDescription = stateText
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            when {
                unavailable -> DeaftlyIcons.MicOff
                on -> DeaftlyIcons.Stop
                else -> DeaftlyIcons.Mic
            },
            contentDescription = null,
            tint = if (unavailable) c.inkSecondary else content,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun VoicePermissionCard(prompt: VoicePermissionPrompt) {
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
        Text(
            text = stringResource(R.string.chat_voice_permission),
            style = MaterialTheme.typography.bodyMedium,
            color = Deaftly.colors.ink,
        )
        if (prompt.blocked) {
            SecondaryButton(
                stringResource(R.string.chat_voice_open_settings),
                prompt.onOpenSettings,
                Modifier.fillMaxWidth(),
                DeaftlyIcons.Settings,
            )
        } else {
            SecondaryButton(stringResource(R.string.chat_voice_allow), prompt.onAllow, Modifier.fillMaxWidth(), DeaftlyIcons.Mic)
        }
    }
}
