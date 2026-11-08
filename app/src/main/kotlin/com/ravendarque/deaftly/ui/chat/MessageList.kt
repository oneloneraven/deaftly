package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Conversation
import com.ravendarque.deaftly.core.chat.Message
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.Span
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.ParticipantColours
import com.ravendarque.deaftly.ui.theme.colours
import com.ravendarque.deaftly.core.chat.SpanStyle as ChatSpanStyle

private const val BUBBLE_MAX_WIDTH_FRACTION = 0.8f
private const val LINE_HEIGHT_RATIO = 1.4f
private const val QUOTE_SIZE_RATIO = 0.8f
private val BubbleRadius = 22.dp
private val TailRadius = 6.dp
private val QuoteBar = 3.dp

@Composable
fun MessageList(
    conversation: Conversation,
    textSize: ChatTextSize,
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
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = Deaftly.spacing.space14, vertical = Deaftly.spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10, Alignment.Bottom),
    ) {
        items(conversation.messages, key = { it.id }) { message ->
            MessageBubble(message, conversation, textSize)
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    conversation: Conversation,
    textSize: ChatTextSize,
) {
    val own = message.author == ParticipantId.A
    val author = conversation.participant(message.author).labelled()
    val colours = author.colour.colours()
    val shape =
        RoundedCornerShape(
            topStart = BubbleRadius,
            topEnd = BubbleRadius,
            bottomEnd = if (own) TailRadius else BubbleRadius,
            bottomStart = if (own) BubbleRadius else TailRadius,
        )
    val description = stringResource(R.string.chat_message_from, author.displayName.orEmpty(), message.plainText)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .align(if (own) Alignment.CenterEnd else Alignment.CenterStart)
                    .widthIn(max = maxWidth * BUBBLE_MAX_WIDTH_FRACTION)
                    .background(colours.container, shape)
                    .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space12)
                    .semantics(mergeDescendants = true) { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
        ) {
            message.quote?.let { QuoteBlock(it.text, colours, textSize) }
            Text(
                text = richText(message.spans),
                color = colours.onContainer,
                style = bubbleStyle(textSize.sp),
            )
        }
    }
}

@Composable
private fun QuoteBlock(
    text: String,
    colours: ParticipantColours,
    textSize: ChatTextSize,
) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(QuoteBar).fillMaxHeight().background(colours.onContainerMuted))
        Text(
            text = text,
            color = colours.onContainerMuted,
            style = bubbleStyle((textSize.sp * QUOTE_SIZE_RATIO).toInt()),
            modifier = Modifier.padding(start = Deaftly.spacing.space10),
        )
    }
}

@Composable
private fun bubbleStyle(size: Int) = MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp, lineHeight = (size * LINE_HEIGHT_RATIO).sp)

@Composable
private fun richText(spans: List<Span>): AnnotatedString {
    val highlight = Deaftly.colors.highlight
    val onHighlight = Deaftly.colors.onHighlight
    return buildAnnotatedString {
        spans.forEach { span ->
            withStyle(
                SpanStyle(
                    fontWeight = if (ChatSpanStyle.Bold in span.styles) FontWeight.Bold else null,
                    fontStyle = if (ChatSpanStyle.Italic in span.styles) FontStyle.Italic else null,
                    background = if (ChatSpanStyle.Highlight in span.styles) highlight else Color.Unspecified,
                    color = if (ChatSpanStyle.Highlight in span.styles) onHighlight else Color.Unspecified,
                ),
            ) { append(span.text) }
        }
    }
}
