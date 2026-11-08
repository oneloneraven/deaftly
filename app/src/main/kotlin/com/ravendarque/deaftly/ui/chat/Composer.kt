package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.Participant
import com.ravendarque.deaftly.core.chat.ParticipantId
import com.ravendarque.deaftly.core.chat.SpanStyle
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.ParticipantColours
import com.ravendarque.deaftly.ui.theme.colours

private val CardBorder = 2.5.dp
private val SendWidth = 52.dp
private val ControlSize = 48.dp
private val InputMinHeight = 44.dp
private val ToolbarIcon = 20.dp
private const val DISABLED_ALPHA = 0.4f
private const val MAX_LINES = 4
private const val QUOTE_LINES = 2
private const val GLYPH_SP = 18

data class ComposerActions(
    val onChange: (Composition) -> Unit,
    val onQuoteLatest: () -> Unit,
    val onRemoveQuote: () -> Unit,
    val onSend: () -> Unit,
)

@Composable
fun Composer(
    participant: Participant,
    draft: Draft,
    canQuote: Boolean,
    actions: ComposerActions,
    modifier: Modifier = Modifier,
) {
    val colours = participant.colour.colours()
    val shape = Deaftly.shapes.card
    val c = Deaftly.colors
    val state = rememberComposerState(participant.id, draft.composition, actions.onChange)
    val canSend = !state.composition.isBlank
    val send = {
        if (canSend) {
            actions.onSend()
        }
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(c.surface, shape)
                .border(CardBorder, colours.strong, shape)
                .padding(
                    start = Deaftly.spacing.space12,
                    end = Deaftly.spacing.space12,
                    top = Deaftly.spacing.space12,
                    bottom = Deaftly.spacing.space10,
                ),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        TurnPrompt(participant.id)
        draft.quote?.let { QuotePreview(it.text, colours.strong, actions.onRemoveQuote) }
        MessageField(state, participant.labelled().displayName.orEmpty(), colours.strong, send)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4),
        ) {
            FormattingToolbar(state, draft, canQuote, actions, colours, Modifier.weight(1f))
            SendButton(enabled = canSend, onClick = send, container = colours.strong, content = colours.onStrong)
        }
    }
}

@Composable
private fun TurnPrompt(id: ParticipantId) {
    val c = Deaftly.colors
    Row(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        Icon(DeaftlyIcons.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = c.ink)
        Text(
            text = stringResource(if (id == ParticipantId.B) R.string.chat_turn_prompt_other else R.string.chat_turn_prompt_own),
            style = MaterialTheme.typography.titleSmall,
            color = c.ink,
        )
    }
}

@Composable
private fun FormattingToolbar(
    state: ComposerState,
    draft: Draft,
    canQuote: Boolean,
    actions: ComposerActions,
    colours: ParticipantColours,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4)) {
        FormatButton(state, SpanStyle.Bold, R.string.chat_format_bold, colours) {
            Text(
                text = stringResource(R.string.chat_format_glyph_bold),
                fontSize = GLYPH_SP.sp,
                fontWeight = FontWeight.ExtraBold,
                color = it,
            )
        }
        FormatButton(state, SpanStyle.Italic, R.string.chat_format_italic, colours) {
            Text(
                text = stringResource(R.string.chat_format_glyph_italic),
                fontSize = GLYPH_SP.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                color = it,
            )
        }
        FormatButton(state, SpanStyle.Highlight, R.string.chat_format_highlight, colours) {
            Icon(DeaftlyIcons.Highlight, contentDescription = null, modifier = Modifier.size(ToolbarIcon), tint = it)
        }
        ToolbarButton(
            label = stringResource(R.string.chat_quote_latest),
            selected = draft.quote != null,
            enabled = canQuote || draft.quote != null,
            colours = colours,
            onClick = if (draft.quote != null) actions.onRemoveQuote else actions.onQuoteLatest,
        ) {
            Icon(DeaftlyIcons.Quote, contentDescription = null, modifier = Modifier.size(ToolbarIcon), tint = it)
        }
    }
}

@Composable
private fun FormatButton(
    state: ComposerState,
    style: SpanStyle,
    label: Int,
    colours: ParticipantColours,
    glyph: @Composable (Color) -> Unit,
) {
    ToolbarButton(
        label = stringResource(label),
        selected = state.composition.isActive(style),
        colours = colours,
        onClick = { state.toggle(style) },
        glyph = glyph,
    )
}

@Composable
private fun ToolbarButton(
    label: String,
    selected: Boolean,
    colours: ParticipantColours,
    onClick: () -> Unit,
    enabled: Boolean = true,
    glyph: @Composable (Color) -> Unit,
) {
    val c = Deaftly.colors
    val stateText = stringResource(if (selected) R.string.chat_format_on else R.string.chat_format_off)
    Box(
        modifier =
            Modifier
                .size(ControlSize)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(Deaftly.shapes.controlSmall)
                .background(if (selected) colours.strong else c.ground)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    this.selected = selected
                    stateDescription = stateText
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.clearAndSetSemantics {}) { glyph(if (selected) colours.onStrong else c.ink) }
    }
}

@Composable
private fun QuotePreview(
    text: String,
    bar: Color,
    onRemove: () -> Unit,
) {
    val description = stringResource(R.string.chat_quote_pending, text)
    val removeLabel = stringResource(R.string.chat_quote_remove)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = description },
        ) {
            QuoteBlock(text, bar, Deaftly.colors.inkSecondary, MaterialTheme.typography.bodyMedium, QUOTE_LINES)
        }
        Box(
            modifier =
                Modifier
                    .size(ControlSize)
                    .clip(Deaftly.shapes.controlSmall)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(DeaftlyIcons.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color,
) {
    val label = stringResource(R.string.chat_send)
    Box(
        modifier =
            Modifier
                .size(width = SendWidth, height = ControlSize)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(RoundedCornerShape(14.dp))
                .background(container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(DeaftlyIcons.Send, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MessageField(
    state: ComposerState,
    owner: String,
    cursor: Color,
    onSend: () -> Unit,
) {
    val c = Deaftly.colors
    val label = stringResource(R.string.chat_composer_label, owner)
    BasicTextField(
        state = state.field,
        modifier = Modifier.fillMaxWidth().heightIn(min = InputMinHeight).semantics { contentDescription = label },
        inputTransformation = state.input,
        outputTransformation = state.output,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
        cursorBrush = SolidColor(cursor),
        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = MAX_LINES),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
        onKeyboardAction = KeyboardActionHandler { onSend() },
        decorator =
            TextFieldDecorator { inner ->
                Box(Modifier.heightIn(min = InputMinHeight), contentAlignment = Alignment.CenterStart) {
                    if (state.field.text.isEmpty()) {
                        Text(stringResource(R.string.chat_composer_hint), style = MaterialTheme.typography.bodyLarge, color = c.inkMuted)
                    }
                    inner()
                }
            },
    )
}
