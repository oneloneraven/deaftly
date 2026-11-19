package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val QUOTE_LINES = 2

@Composable
internal fun DraftContext(
    draft: Draft,
    bar: Color,
    actions: ComposerActions,
) {
    draft.editing?.let { EditingBar(actions.onCancelEdit) }
    draft.quote?.let { QuotePreview(it.text, bar, actions.onRemoveQuote) }
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
private fun EditingBar(onCancel: () -> Unit) {
    val cancelLabel = stringResource(R.string.chat_cancel_editing)
    Row(verticalAlignment = Alignment.CenterVertically) {
        PromptLine(DeaftlyIcons.Edit, R.string.chat_editing_message, announce = true, modifier = Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .size(ControlSize)
                    .clip(Deaftly.shapes.controlSmall)
                    .clickable(role = Role.Button, onClick = onCancel)
                    .semantics { contentDescription = cancelLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(DeaftlyIcons.Close, contentDescription = null, modifier = Modifier.size(18.dp), tint = Deaftly.colors.ink)
        }
    }
}
