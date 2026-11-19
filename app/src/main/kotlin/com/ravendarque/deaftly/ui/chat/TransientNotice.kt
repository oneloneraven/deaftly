package com.ravendarque.deaftly.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.ravendarque.deaftly.ui.theme.Deaftly
import kotlinx.coroutines.delay

internal const val NOTICE_MILLIS = 2_500L

@Composable
internal fun TransientNotice(
    trigger: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            visible = true
            delay(NOTICE_MILLIS)
            visible = false
        }
    }
    AnimatedVisibility(visible, modifier, enter = fadeIn(), exit = fadeOut()) {
        val c = Deaftly.colors
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = c.surface,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .clip(Deaftly.shapes.button)
                    .background(c.ink)
                    .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space10)
                    .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
