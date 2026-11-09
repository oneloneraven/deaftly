package com.ravendarque.deaftly.ui.facetoface

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.hintedFieldDecorator
import com.ravendarque.deaftly.ui.editor.ConfirmDialog
import com.ravendarque.deaftly.ui.editor.ConfirmSpec
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val HALF_ROTATION = 180f
private const val OWN_INDEX = 0f
private const val STRANGER_INDEX = 1f
private const val BAR_INDEX = 2f
private const val MAX_FIELD_LINES = 4
private const val SCROLL_TO_END = 1_000_000f
private val BarTarget = 48.dp
private val SendHeight = 48.dp
private val SendWidth = 52.dp
private const val DISABLED_ALPHA = 0.4f
private val CaretWidth = 3.dp
private val FieldBorder = 2.dp

@Composable
fun FaceToFaceScreen(
    viewModel: FaceToFaceViewModel,
    onEnd: () -> Unit,
    onContinue: (Route.Chat) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val field = remember { TextFieldState(state.draft, TextRange(state.draft.length)) }
    LaunchedEffect(field) { snapshotFlow { field.text.toString() }.collect(viewModel::updateDraft) }
    FaceToFaceContent(
        messages = state.messages,
        field = field,
        onSend = { text ->
            viewModel.send(text)
            field.clearText()
        },
        onEnd = onEnd,
        onContinue = { onContinue(viewModel.continueAsChat(field.text.toString())) },
    )
}

@Composable
fun FaceToFaceContent(
    messages: List<String>,
    field: TextFieldState,
    onSend: (String) -> Unit,
    onEnd: () -> Unit,
    onContinue: () -> Unit,
) {
    var confirmingEnd by rememberSaveable { mutableStateOf(false) }
    val requestEnd = { if (messages.isEmpty()) onEnd() else confirmingEnd = true }
    BackHandler(onBack = requestEnd)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            TopBar(requestEnd, onContinue, Modifier.semantics { traversalIndex = BAR_INDEX })
            StrangerHalf(
                messages = messages,
                live = field.text.toString(),
                modifier = Modifier.weight(1f).semantics { traversalIndex = STRANGER_INDEX },
            )
            OwnHalf(field, onSend, Modifier.semantics { traversalIndex = OWN_INDEX })
        }
    }
    if (confirmingEnd) {
        ConfirmDialog(
            spec =
                ConfirmSpec(
                    title = stringResource(R.string.chat_end_title),
                    text = stringResource(R.string.chat_end_text),
                    confirm = stringResource(R.string.chat_end_confirm),
                    dismiss = stringResource(R.string.chat_end_keep),
                    destructive = true,
                ),
            onConfirm = {
                confirmingEnd = false
                onEnd()
            },
            onDismiss = { confirmingEnd = false },
        )
    }
}

@Composable
private fun TopBar(
    onEnd: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { isTraversalGroup = true }
                .padding(horizontal = Deaftly.spacing.space12, vertical = Deaftly.spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DeaftlyIconButton(DeaftlyIcons.Close, stringResource(R.string.f2f_close), onEnd)
        Row(
            modifier =
                Modifier
                    .heightIn(min = BarTarget)
                    .clip(Deaftly.shapes.controlSmall)
                    .clickable(role = Role.Button, onClick = onContinue)
                    .padding(horizontal = Deaftly.spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
        ) {
            Icon(DeaftlyIcons.SideBySide, contentDescription = null, modifier = Modifier.size(20.dp), tint = Deaftly.colors.ink)
            Text(
                text = stringResource(R.string.f2f_continue),
                style = MaterialTheme.typography.labelLarge,
                color = Deaftly.colors.ink,
            )
        }
    }
}

@Composable
private fun StrangerHalf(
    messages: List<String>,
    live: String,
    modifier: Modifier,
) {
    val list = rememberLazyListState()
    var following by remember { mutableStateOf(true) }
    LaunchedEffect(list) {
        snapshotFlow { list.isScrollInProgress }.collect { scrolling -> if (!scrolling) following = !list.canScrollForward }
    }
    LaunchedEffect(messages.size) { following = true }
    LaunchedEffect(messages.size, live) {
        if (following) list.scrollBy(SCROLL_TO_END)
    }
    val description = stringResource(R.string.f2f_stranger_list)
    val liveLabel = stringResource(R.string.f2f_live, live)
    val c = Deaftly.colors
    LazyColumn(
        state = list,
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { rotationZ = HALF_ROTATION }
                .background(c.surfaceSunken)
                .semantics {
                    isTraversalGroup = true
                    contentDescription = description
                },
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
        contentPadding = PaddingValues(Deaftly.spacing.space16),
    ) {
        if (messages.isEmpty() && live.isBlank()) {
            item(key = "welcome") {
                Text(stringResource(R.string.f2f_welcome), style = MaterialTheme.typography.displaySmall, color = c.inkMuted)
            }
        }
        itemsIndexed(messages, key = { index, _ -> index }) { _, message ->
            Text(message, style = MaterialTheme.typography.displaySmall, color = c.inkSecondary)
        }
        if (live.isNotBlank()) {
            item(key = "live") {
                val caret = c.accent
                val caretWidth = CaretWidth
                Text(
                    text = live,
                    style = MaterialTheme.typography.displaySmall,
                    color = c.ink,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .drawBehind { drawLine(caret, Offset(0f, 0f), Offset(0f, size.height), caretWidth.toPx()) }
                            .padding(start = Deaftly.spacing.space12)
                            .semantics { contentDescription = liveLabel },
                )
            }
        }
    }
}

@Composable
private fun OwnHalf(
    field: TextFieldState,
    onSend: (String) -> Unit,
    modifier: Modifier,
) {
    val c = Deaftly.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canSend = field.text.isNotBlank()
    val send = { if (canSend) onSend(field.text.toString()) }
    val shape = Deaftly.shapes.card
    val label = stringResource(R.string.f2f_field)
    val textStyle = MaterialTheme.typography.bodyLarge
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Deaftly.spacing.space12)
                .then(modifier)
                .semantics { isTraversalGroup = true }
                .clip(shape)
                .background(c.surface)
                .border(FieldBorder, c.accent, shape)
                .padding(FieldBorder),
    ) {
        FieldWithSend(
            field = {
                BasicTextField(
                    state = field,
                    modifier =
                        Modifier
                            .padding(start = Deaftly.spacing.space14, end = Deaftly.spacing.space8)
                            .focusRequester(focus)
                            .semantics { contentDescription = label },
                    textStyle = textStyle.copy(color = c.ink),
                    cursorBrush = SolidColor(c.accent),
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = MAX_FIELD_LINES),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    onKeyboardAction = KeyboardActionHandler { send() },
                    decorator =
                        hintedFieldDecorator(
                            isEmpty = field.text.isEmpty(),
                            hint = stringResource(R.string.f2f_hint),
                            textStyle = textStyle,
                            hintColor = c.inkMuted,
                            minHeight = SendHeight,
                        ),
                )
            },
            send = { SendSegment(enabled = canSend, onClick = send, container = c.accent, content = c.onAccent) },
        )
    }
}

@Composable
private fun FieldWithSend(
    field: @Composable () -> Unit,
    send: @Composable () -> Unit,
) {
    Layout(content = {
        field()
        send()
    }) { measurables, constraints ->
        val sendWidth = SendWidth.roundToPx()
        val fieldPlaceable = measurables[0].measure(Constraints(maxWidth = constraints.maxWidth - sendWidth))
        val height = maxOf(fieldPlaceable.height, SendHeight.roundToPx())
        val sendPlaceable = measurables[1].measure(Constraints.fixed(sendWidth, height))
        layout(constraints.maxWidth, height) {
            fieldPlaceable.placeRelative(0, (height - fieldPlaceable.height) / 2)
            sendPlaceable.placeRelative(constraints.maxWidth - sendWidth, 0)
        }
    }
}

@Composable
private fun SendSegment(
    enabled: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
) {
    val label = stringResource(R.string.chat_send)
    Box(
        modifier =
            Modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(DeaftlyIcons.Send, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}
