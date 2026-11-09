package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
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
import com.ravendarque.deaftly.core.settings.ChatTextSize
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.ParticipantColours
import com.ravendarque.deaftly.ui.theme.colours

private const val QUOTE_SIZE_RATIO = 0.8f
private const val QUOTE_MAX_LINES = 3
private const val SELECTION_ALPHA = 0.3f
private const val LABEL_TRACKING = 0.04f
private val BarWidth = 4.dp
private val DividerHeight = 1.dp
private val QuoteIndent = 12.dp

@Composable
fun MessageList(
    conversation: Conversation,
    textSize: ChatTextSize,
    sidesSwapped: Boolean,
    onQuote: ((messageId: Int, excerpt: String?) -> Unit)?,
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
            ScriptMessage(message, conversation, textSize, message.author.isOnRight(sidesSwapped), onQuote)
        }
    }
}

@Composable
private fun MessageDivider() {
    Box(Modifier.fillMaxWidth().height(DividerHeight).background(Deaftly.colors.line))
}

@Composable
private fun ScriptMessage(
    message: Message,
    conversation: Conversation,
    textSize: ChatTextSize,
    onRight: Boolean,
    onQuote: ((messageId: Int, excerpt: String?) -> Unit)?,
) {
    val author = conversation.participant(message.author).labelled()
    val colours = author.colour.colours()
    val quoteLabel = stringResource(R.string.chat_quote_action)
    val said = stringResource(R.string.chat_message_from, author.displayName.orEmpty(), message.plainText)
    val description = message.quote?.let { stringResource(R.string.chat_message_quoting, it.text, said) } ?: said
    val align = if (onRight) TextAlign.End else TextAlign.Start
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = Deaftly.spacing.space8)
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    traversalIndex = message.id.toFloat()
                    if (onQuote != null) {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(quoteLabel) {
                                    onQuote(message.id, null)
                                    true
                                },
                            )
                    }
                },
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        if (!onRight) SpeakerBar(colours.strong)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (onRight) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4),
        ) {
            NameLabel(author.displayName.orEmpty(), colours.label, align)
            message.quote?.let { QuoteLine(it.text, textSize, align, onRight) }
            SelectableText(message, colours, textSize, align, onQuote)
        }
        if (onRight) SpeakerBar(colours.strong)
    }
}

@Composable
private fun SpeakerBar(colour: Color) {
    Box(Modifier.width(BarWidth).fillMaxHeight().background(colour, RoundedCornerShape(BarWidth / 2)))
}

@Composable
private fun NameLabel(
    name: String,
    colour: Color,
    align: TextAlign,
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
private fun SelectableText(
    message: Message,
    colours: ParticipantColours,
    textSize: ChatTextSize,
    align: TextAlign,
    onQuote: ((messageId: Int, excerpt: String?) -> Unit)?,
) {
    val quoteLabel = stringResource(R.string.chat_quote_action)
    val formatting = rememberChatFormatting()
    val selection = rememberSelectionState()
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(colours.label, colours.label.copy(alpha = SELECTION_ALPHA)),
    ) {
        SelectionContainer(
            state = selection,
            modifier =
                Modifier.appendTextContextMenuComponents {
                    if (onQuote != null) {
                        item(QuoteMenuKey, quoteLabel) {
                            onQuote(message.id, selection.selectedTexts.joinToString("") { it.text })
                            selection.clear()
                            close()
                        }
                    }
                },
        ) {
            Text(
                text = formatting.annotate(message.spans),
                color = Deaftly.colors.ink,
                style = chatTextStyle(textSize.sp),
                textAlign = align,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private data object QuoteMenuKey
