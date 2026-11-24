package com.oneloneraven.deaftly.ui.listen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.navigation.Route
import com.oneloneraven.deaftly.ui.chat.VoicePermissionCard
import com.oneloneraven.deaftly.ui.chat.VoicePermissionPrompt
import com.oneloneraven.deaftly.ui.chat.chatTextStyle
import com.oneloneraven.deaftly.ui.components.DeaftlyIconButton
import com.oneloneraven.deaftly.ui.components.SecondaryButton
import com.oneloneraven.deaftly.ui.editor.ConfirmDialog
import com.oneloneraven.deaftly.ui.editor.ConfirmSpec
import com.oneloneraven.deaftly.ui.icons.DeaftlyIcons
import com.oneloneraven.deaftly.ui.permissions.PermissionAccess
import com.oneloneraven.deaftly.ui.permissions.RuntimePermission
import com.oneloneraven.deaftly.ui.permissions.rememberMicrophonePermission
import com.oneloneraven.deaftly.ui.theme.Deaftly

private const val HALF_ROTATION = 180f
private const val QUARTER_ROTATION = 90f
private const val SCROLL_TO_END = 100_000f
private val CaretWidth = 3.dp

@Composable
fun ListenScreen(
    viewModel: ListenViewModel,
    onEnd: () -> Unit,
    onContinue: (Route.Chat) -> Unit,
    microphone: RuntimePermission = rememberMicrophonePermission(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val granted = microphone.access == PermissionAccess.Granted
    LifecycleResumeEffect(granted) {
        viewModel.setListeningAllowed(granted)
        onPauseOrDispose { viewModel.setListeningAllowed(false) }
    }
    LaunchedEffect(Unit) {
        if (microphone.access == PermissionAccess.NotAsked) microphone.request()
    }
    val prompt =
        if (granted) {
            null
        } else {
            VoicePermissionPrompt(microphone.access == PermissionAccess.DeniedForGood, microphone.request, microphone.openSettings)
        }
    ListenContent(
        state = state,
        permissionPrompt = prompt,
        actions =
            ListenActions(
                onEnd = onEnd,
                onFlip = viewModel::flipSign,
                onToggleListening = viewModel::toggleListening,
                onContinue = { onContinue(viewModel.continueAsChat()) },
                isEmpty = { viewModel.isEmpty },
            ),
    )
}

class ListenActions(
    val onEnd: () -> Unit,
    val onFlip: () -> Unit,
    val onToggleListening: () -> Unit,
    val onContinue: () -> Unit,
    val isEmpty: () -> Boolean,
)

@Composable
fun ListenContent(
    state: ListenUiState,
    permissionPrompt: VoicePermissionPrompt?,
    actions: ListenActions,
) {
    var confirmingEnd by rememberSaveable { mutableStateOf(false) }
    val requestEnd = { if (actions.isEmpty()) actions.onEnd() else confirmingEnd = true }
    BackHandler(onBack = requestEnd)
    KeepScreenOn()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Sign(state.signFacesThem)
            TopBar(state, permissionPrompt == null, requestEnd, actions)
            Transcript(state, permissionPrompt, Modifier.weight(1f))
            BottomBar(actions.onContinue)
        }
    }
    if (confirmingEnd) {
        ConfirmDialog(
            spec =
                ConfirmSpec(
                    title = stringResource(R.string.listen_end_title),
                    text = stringResource(R.string.listen_end_text),
                    confirm = stringResource(R.string.listen_end_confirm),
                    dismiss = stringResource(R.string.listen_end_keep),
                    destructive = true,
                ),
            onConfirm = {
                confirmingEnd = false
                actions.onEnd()
            },
            onDismiss = { confirmingEnd = false },
        )
    }
}

@Composable
private fun Sign(facesThem: Boolean) {
    val c = Deaftly.colors
    Text(
        text = stringResource(R.string.listen_sign),
        style = MaterialTheme.typography.headlineMedium,
        color = c.ink,
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = if (facesThem) HALF_ROTATION else 0f }
                .background(c.accentSoft)
                .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space24)
                .semantics { heading() },
    )
}

@Composable
private fun TopBar(
    state: ListenUiState,
    canListen: Boolean,
    onEnd: () -> Unit,
    actions: ListenActions,
) {
    val facesThem = state.signFacesThem
    val listening = state.voice.on
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { isTraversalGroup = true }
                .padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeaftlyIconButton(DeaftlyIcons.Close, stringResource(R.string.listen_close), onEnd)
        Row {
            DeaftlyIconButton(
                if (listening) DeaftlyIcons.Stop else DeaftlyIcons.Mic,
                stringResource(if (listening) R.string.listen_pause else R.string.listen_resume),
                actions.onToggleListening,
                enabled = canListen && !state.voice.unavailable,
            )
            DeaftlyIconButton(
                DeaftlyIcons.SwitchSides,
                stringResource(if (facesThem) R.string.listen_flip_to_you else R.string.listen_flip_to_them),
                actions.onFlip,
                Modifier.rotate(QUARTER_ROTATION),
            )
        }
    }
}

@Composable
private fun Transcript(
    state: ListenUiState,
    permissionPrompt: VoicePermissionPrompt?,
    modifier: Modifier,
) {
    val list = rememberLazyListState()
    val live = state.voice.live
    LaunchedEffect(state.heard.size, live) { list.scrollBy(SCROLL_TO_END) }
    val c = Deaftly.colors
    val style = chatTextStyle(state.textSize.sp)
    val description = stringResource(R.string.listen_transcript)
    val liveLabel = stringResource(R.string.listen_live, live)
    LazyColumn(
        state = list,
        modifier =
            modifier.fillMaxWidth().semantics {
                isTraversalGroup = true
                contentDescription = description
            },
        contentPadding = PaddingValues(Deaftly.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        if (permissionPrompt != null) {
            item(key = "permission") { VoicePermissionCard(permissionPrompt) }
        }
        itemsIndexed(state.heard, key = { index, _ -> index }) { _, line ->
            Text(line, style = style, color = c.ink)
        }
        if (live.isNotBlank()) {
            item(key = "live") {
                val caret = c.accent
                Text(
                    text = live,
                    style = style,
                    color = c.inkSecondary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .drawBehind { drawLine(caret, Offset(0f, 0f), Offset(0f, size.height), CaretWidth.toPx()) }
                            .padding(start = Deaftly.spacing.space12)
                            .semantics { contentDescription = liveLabel },
                )
            }
        }
        if (permissionPrompt == null && state.needsStatus) {
            item(key = "status") { Status(state) }
        }
    }
}

private val ListenUiState.needsStatus: Boolean
    get() = (heard.isEmpty() && voice.live.isBlank()) || !voice.on || voice.unavailable

@Composable
private fun Status(state: ListenUiState) {
    val text =
        when {
            state.voice.unavailable -> R.string.listen_unavailable
            state.voice.on -> R.string.listen_waiting
            else -> R.string.listen_paused
        }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        color = Deaftly.colors.inkMuted,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun BottomBar(onContinue: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space12),
        contentAlignment = Alignment.Center,
    ) {
        SecondaryButton(stringResource(R.string.f2f_continue), onContinue, icon = DeaftlyIcons.SideBySide)
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
