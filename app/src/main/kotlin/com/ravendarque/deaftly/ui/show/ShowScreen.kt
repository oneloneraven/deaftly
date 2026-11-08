package com.ravendarque.deaftly.ui.show

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.LocalDarkTheme

private const val GESTURE_GAP_NANOS = 300_000_000L
private val SwipeToLeave = 96.dp
private val BarTarget = 48.dp
private val ChatButtonHeight = 52.dp
private val ChatButtonBorder = 2.dp

@Composable
fun ShowScreen(
    text: String,
    onClose: () -> Unit,
    onContinueAsChat: () -> Unit,
    colours: ShowColours = defaultShowColours(LocalDarkTheme.current),
) {
    KeepScreenOn()
    ImmersiveMode()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colours.background)
                .safeDrawingPadding()
                .leaveOnSwipeDown(onClose),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(BarTarget),
                colors = IconButtonDefaults.iconButtonColors(contentColor = colours.muted),
            ) {
                Icon(DeaftlyIcons.Close, contentDescription = stringResource(R.string.show_close))
            }
        }
        AutoSizedMessage(
            text = text,
            color = colours.text,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 26.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = onContinueAsChat,
                modifier = Modifier.heightIn(min = ChatButtonHeight),
                border = BorderStroke(ChatButtonBorder, colours.muted),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colours.text),
            ) {
                Icon(DeaftlyIcons.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.show_continue_chat),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = stringResource(R.string.show_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colours.muted,
            )
        }
    }
}

@Composable
private fun Modifier.leaveOnSwipeDown(onLeave: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { SwipeToLeave.toPx() }
    val currentOnLeave by rememberUpdatedState(onLeave)
    val gesture = remember { SwipeTracker() }
    val state =
        rememberScrollableState { delta ->
            if (gesture.add(delta, threshold, System.nanoTime())) currentOnLeave()
            0f
        }
    return scrollable(state, Orientation.Vertical)
}

private class SwipeTracker {
    private var travelled = 0f
    private var lastNanos = 0L
    private var fired = false

    fun add(
        delta: Float,
        threshold: Float,
        nowNanos: Long,
    ): Boolean {
        if (nowNanos - lastNanos > GESTURE_GAP_NANOS) {
            travelled = 0f
            fired = false
        }
        lastNanos = nowNanos
        travelled = if (delta > 0f) travelled + delta else 0f
        if (fired || travelled < threshold) return false
        fired = true
        return true
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
