package com.ravendarque.deaftly.ui.chat

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Message
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.ParticipantColours
import com.ravendarque.deaftly.ui.theme.colours
import kotlinx.coroutines.launch

private const val QUOTE_SIZE_RATIO = 0.8f
private const val QUOTE_MAX_LINES = 3
private const val LABEL_TRACKING = 0.04f
private val BarWidth = 4.dp
private val BarGap = 12.dp
private val PointDepth = 6.dp
private val PointHeight = 10.dp
private val DividerHeight = 1.dp
private val QuoteIndent = 12.dp

@Composable
fun MessageList(
    conversation: Conversation,
    textSize: ChatTextSize,
    sidesSwapped: Boolean,
    actions: MessageActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val count = conversation.messages.size
    if (count == 0) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.chat_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Deaftly.colors.inkMuted,
            )
        }
        return
    }
    LaunchedEffect(count, textSize) {
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    LazyColumn(
        modifier = modifier.semantics { isTraversalGroup = true },
        state = listState,
        contentPadding = PaddingValues(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space8),
        verticalArrangement = Arrangement.Bottom,
    ) {
        itemsIndexed(conversation.messages, key = { _, message -> message.id }) { index, message ->
            if (index > 0) MessageDivider()
            ScriptMessage(message, conversation, textSize, message.author.isOnRight(sidesSwapped), actions)
        }
    }
}

@Composable
private fun MessageDivider() {
    Box(Modifier.fillMaxWidth().height(DividerHeight).background(Deaftly.colors.line))
}

private data class MessageCommand(
    val label: String,
    val destructive: Boolean = false,
    val run: () -> Unit,
)

@Composable
private fun rememberMessageCommands(
    message: Message,
    actions: MessageActions,
): List<MessageCommand> {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copyLabel = stringResource(R.string.chat_copy_action)
    val quoteLabel = stringResource(R.string.chat_quote_action)
    val deleteLabel = stringResource(R.string.chat_delete_action)
    return listOf(
        MessageCommand(copyLabel) {
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, message.plainText))) }
        },
        MessageCommand(quoteLabel) { actions.onQuote(message.id) },
        MessageCommand(deleteLabel, destructive = true) { actions.onDelete(message.id) },
    )
}

@Composable
private fun ScriptMessage(
    message: Message,
    conversation: Conversation,
    textSize: ChatTextSize,
    onRight: Boolean,
    actions: MessageActions,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val commands = rememberMessageCommands(message, actions)
    val menuLabel = stringResource(R.string.chat_message_actions)
    val author = conversation.participant(message.author).labelled()
    val said = stringResource(R.string.chat_message_from, author.displayName.orEmpty(), message.plainText)
    val description = message.quote?.let { stringResource(R.string.chat_message_quoting, it.text, said) } ?: said
    Box {
        ScriptLine(
            message = message,
            author = author,
            textSize = textSize,
            onRight = onRight,
            modifier =
                Modifier
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { menuOpen = true }) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        traversalIndex = message.id.toFloat()
                        onLongClick(menuLabel) {
                            menuOpen = true
                            true
                        }
                        customActions =
                            commands.map { command ->
                                CustomAccessibilityAction(command.label) {
                                    command.run()
                                    true
                                }
                            }
                    },
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            commands.forEach { command ->
                DropdownMenuItem(
                    text = { Text(command.label, color = if (command.destructive) Deaftly.colors.error else Color.Unspecified) },
                    onClick = {
                        menuOpen = false
                        command.run()
                    },
                )
            }
        }
    }
}

@Composable
private fun ScriptLine(
    message: Message,
    author: Participant,
    textSize: ChatTextSize,
    onRight: Boolean,
    modifier: Modifier,
) {
    val colours = author.colour.colours()
    val align = if (onRight) TextAlign.End else TextAlign.Start
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = Deaftly.spacing.space8),
        horizontalArrangement = Arrangement.spacedBy(BarGap),
    ) {
        if (!onRight) SpeakerBar(colours.strong)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (onRight) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4),
        ) {
            NameLabel(author.displayName.orEmpty(), colours.label, align, SpeechPoint(colours.strong, onRight))
            message.quote?.let { QuoteLine(it.text, textSize, align, onRight) }
            MessageText(message, textSize, align)
        }
        if (onRight) SpeakerBar(colours.strong)
    }
}

@Composable
private fun SpeakerBar(colour: Color) {
    Box(Modifier.width(BarWidth).fillMaxHeight().background(colour, RoundedCornerShape(BarWidth / 2)))
}

data class MessageActions(
    val onQuote: (messageId: Int) -> Unit,
    val onDelete: (messageId: Int) -> Unit,
)

private data class SpeechPoint(
    val colour: Color,
    val onRight: Boolean,
)

private fun Modifier.speechPoint(point: SpeechPoint): Modifier =
    drawBehind {
        val gap = BarGap.toPx()
        val depth = PointDepth.toPx()
        val half = PointHeight.toPx() / 2
        val centre = size.height / 2
        val base = if (point.onRight) size.width + gap else -gap
        val tip = if (point.onRight) base - depth else base + depth
        val path =
            Path().apply {
                moveTo(base, centre - half)
                lineTo(tip, centre)
                lineTo(base, centre + half)
                close()
            }
        drawPath(path, point.colour)
    }

@Composable
private fun NameLabel(
    name: String,
    colour: Color,
    align: TextAlign,
    point: SpeechPoint,
) {
    Box(
        modifier = Modifier.fillMaxWidth().speechPoint(point),
        contentAlignment = if (point.onRight) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Text(
            text = name.uppercase(),
            color = colour,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = LABEL_TRACKING.em),
            fontWeight = FontWeight.Medium,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuoteLine(
    text: String,
    textSize: ChatTextSize,
    align: TextAlign,
    onRight: Boolean,
) {
    Text(
        text = text,
        color = Deaftly.colors.inkMuted,
        style = chatTextStyle((textSize.sp * QUOTE_SIZE_RATIO).toInt()),
        textAlign = align,
        maxLines = QUOTE_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(if (onRight) PaddingValues(end = QuoteIndent) else PaddingValues(start = QuoteIndent)),
    )
}

@Composable
private fun MessageText(
    message: Message,
    textSize: ChatTextSize,
    align: TextAlign,
) {
    val formatting = rememberChatFormatting()
    Text(
        text = formatting.annotate(message.spans),
        color = Deaftly.colors.ink,
        style = chatTextStyle(textSize.sp),
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
}
