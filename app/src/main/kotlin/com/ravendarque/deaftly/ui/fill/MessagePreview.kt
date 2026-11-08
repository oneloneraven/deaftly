package com.ravendarque.deaftly.ui.fill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.ui.components.AddMoreChip
import com.ravendarque.deaftly.ui.components.ChipState
import com.ravendarque.deaftly.ui.components.ChipTone
import com.ravendarque.deaftly.ui.components.PlaceholderChip
import com.ravendarque.deaftly.ui.theme.Deaftly

private val WordGap = 7.sp
private val WordPattern = Regex("""(\s*)(\S+)""")

internal sealed interface PreviewToken {
    val gapBefore: Boolean

    data class Word(
        val text: String,
        override val gapBefore: Boolean,
    ) : PreviewToken

    data class Chip(
        val placeholder: Placeholder,
        val label: String,
        val filled: Boolean,
        override val gapBefore: Boolean,
        val suffix: String = "",
        val removeIndex: Int? = null,
    ) : PreviewToken

    data class AddMore(
        val placeholder: Placeholder,
        override val gapBefore: Boolean,
    ) : PreviewToken
}

internal fun previewTokens(parts: List<FillPart>): List<PreviewToken> {
    val tokens = mutableListOf<PreviewToken>()
    var trailingSpace = false
    parts.forEach { part ->
        when (part) {
            is FillPart.Text -> {
                WordPattern.findAll(part.text).forEachIndexed { index, match ->
                    val (space, word) = match.destructured
                    val previous = tokens.lastOrNull()
                    if (index == 0 && space.isEmpty() && previous is PreviewToken.Chip) {
                        tokens[tokens.lastIndex] = previous.copy(suffix = previous.suffix + word)
                    } else {
                        tokens += PreviewToken.Word(word, gapBefore = space.isNotEmpty())
                    }
                }
                if (part.text.isNotEmpty()) trailingSpace = part.text.last().isWhitespace()
            }

            is FillPart.Filled -> {
                tokens.addFilled(part, trailingSpace || (tokens.isNotEmpty() && tokens.last() !is PreviewToken.Word))
                trailingSpace = false
            }

            is FillPart.Empty -> {
                val gap = trailingSpace || (tokens.isNotEmpty() && tokens.last() !is PreviewToken.Word)
                tokens += PreviewToken.Chip(part.placeholder, part.placeholder.key, false, gap)
                trailingSpace = false
            }
        }
    }
    return tokens
}

private fun MutableList<PreviewToken>.addFilled(
    part: FillPart.Filled,
    gapBefore: Boolean,
) {
    if (!part.placeholder.repeatable) {
        this += PreviewToken.Chip(part.placeholder, part.values.first(), true, gapBefore)
        return
    }
    val last = part.values.lastIndex
    part.values.forEachIndexed { index, value ->
        if (index > 0 && index == last) this += PreviewToken.Word("and", gapBefore = true)
        this +=
            PreviewToken.Chip(
                placeholder = part.placeholder,
                label = value,
                filled = true,
                gapBefore = index > 0 || gapBefore,
                suffix = if (index < last - 1) "," else "",
                removeIndex = index,
            )
    }
    this += PreviewToken.AddMore(part.placeholder, gapBefore = true)
}

@Composable
fun MessagePreview(
    parts: List<FillPart>,
    text: String,
    activeKey: String?,
    onActivate: (String) -> Unit,
    onRemove: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = remember(parts) { previewTokens(parts) }
    val gap = with(LocalDensity.current) { WordGap.toDp() }
    val wordStyle = MaterialTheme.typography.displaySmall
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Deaftly.colors.surface, Deaftly.shapes.cardLarge)
                .padding(Deaftly.spacing.space22)
                .semantics { this.text = AnnotatedString(text) },
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        tokens.forEachIndexed { index, token ->
            val spacing = Modifier.padding(end = if (tokens.getOrNull(index + 1)?.gapBefore == true) gap else 0.dp)
            when (token) {
                is PreviewToken.Word -> {
                    Text(
                        text = token.text,
                        style = wordStyle,
                        color = Deaftly.colors.ink,
                        modifier = spacing.clearAndSetSemantics { },
                    )
                }

                is PreviewToken.Chip -> {
                    ChipItem(token, activeKey, onActivate, onRemove, spacing)
                }

                is PreviewToken.AddMore -> {
                    AddMoreChip(
                        label = stringResource(R.string.fill_add_more),
                        onClick = { onActivate(token.placeholder.key) },
                        modifier = spacing,
                        tone = token.placeholder.tone(),
                    )
                }
            }
        }
    }
}

private fun Placeholder.tone() = if (type == PlaceholderType.Camera) ChipTone.PersonB else ChipTone.Accent

@Composable
private fun ChipItem(
    token: PreviewToken.Chip,
    activeKey: String?,
    onActivate: (String) -> Unit,
    onRemove: (String, Int) -> Unit,
    modifier: Modifier,
) {
    val style = MaterialTheme.typography.displaySmall
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PlaceholderChip(
            label = token.label,
            onClick = { onActivate(token.placeholder.key) },
            state =
                when {
                    token.placeholder.key == activeKey -> ChipState.Focused
                    token.filled -> ChipState.Filled
                    else -> ChipState.Empty
                },
            tone = token.placeholder.tone(),
            onRemove = token.removeIndex?.let { removeIndex -> { onRemove(token.placeholder.key, removeIndex) } },
            textStyle = style.copy(fontWeight = FontWeight.Bold),
        )
        if (token.suffix.isNotEmpty()) {
            Text(text = token.suffix, style = style, color = Deaftly.colors.ink, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}
