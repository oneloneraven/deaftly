package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Message
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.ParticipantColours
import com.ravendarque.deaftly.ui.theme.colours

private const val BUBBLE_MAX_WIDTH_FRACTION = 0.8f
private const val LINE_HEIGHT_RATIO = 1.4f
private const val QUOTE_SIZE_RATIO = 0.8f
private val BubbleRadius = 22.dp
private val TailRadius = 6.dp
private const val QUOTE_MAX_LINES = 3
private const val SELECTION_ALPHA = 0.4f

@Composable
fun MessageList(
    conversation: Conversation,
    textSize: ChatTextSize,
    onQuote: ((messageId: Int, excerpt: String?) -> Unit)?,
    modifier: Modifier = Modifier,
    reader: ParticipantId = ParticipantId.A,
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
        contentPadding = PaddingValues(horizontal = Deaftly.spacing.space14, vertical = Deaftly.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10, Alignment.Bottom),
    ) {
        items(conversation.messages, key = { it.id }) { message ->
            MessageBubble(message, conversation, textSize, reader, onQuote)
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    conversation: Conversation,
    textSize: ChatTextSize,
    reader: ParticipantId,
    onQuote: ((messageId: Int, excerpt: String?) -> Unit)?,
) {
    val own = message.author == reader
    val author = conversation.participant(message.author).labelled()
    val colours = author.colour.colours()
    val quoteLabel = stringResource(R.string.chat_quote_action)
    val shape =
        RoundedCornerShape(
            topStart = BubbleRadius,
            topEnd = BubbleRadius,
            bottomEnd = if (own) TailRadius else BubbleRadius,
            bottomStart = if (own) BubbleRadius else TailRadius,
        )
    val said = stringResource(R.string.chat_message_from, author.displayName.orEmpty(), message.plainText)
    val description = message.quote?.let { stringResource(R.string.chat_message_quoting, it.text, said) } ?: said
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .align(if (own) Alignment.CenterEnd else Alignment.CenterStart)
                    .widthIn(max = maxWidth * BUBBLE_MAX_WIDTH_FRACTION)
                    .background(colours.container, shape)
                    .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space12)
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
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
        ) {
            message.quote?.let {
                QuoteBlock(
                    text = it.text,
                    bar = colours.onContainerMuted,
                    colour = colours.onContainerMuted,
                    style = bubbleStyle((textSize.sp * QUOTE_SIZE_RATIO).toInt()),
                    maxLines = QUOTE_MAX_LINES,
                )
            }
            SelectableText(message, colours, textSize, quoteLabel, onQuote)
        }
    }
}

@Composable
private fun SelectableText(
    message: Message,
    colours: ParticipantColours,
    textSize: ChatTextSize,
    quoteLabel: String,
    onQuote: ((messageId: Int, excerpt: String?) -> Unit)?,
) {
    val formatting = rememberChatFormatting()
    val selection = rememberSelectionState()
    val selectionColours = LocalTextSelectionColors.current
    CompositionLocalProvider(
        LocalTextSelectionColors provides
            TextSelectionColors(selectionColours.handleColor, colours.onContainer.copy(alpha = SELECTION_ALPHA)),
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
                color = colours.onContainer,
                style = bubbleStyle(textSize.sp),
            )
        }
    }
}

private data object QuoteMenuKey

@Composable
private fun bubbleStyle(size: Int) = MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp, lineHeight = (size * LINE_HEIGHT_RATIO).sp)
