package com.ravendarque.deaftly.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.TemplateParser
import com.ravendarque.deaftly.ui.theme.Deaftly
import com.ravendarque.deaftly.ui.theme.DeaftlyColors

private const val NO_BREAK_SPACE = ' '
private val MessageTextSize = 20.sp
private val MessageLineHeight = 32.sp
private val MessageMinHeight = 112.dp

private class TokenTransformation(
    private val colors: DeaftlyColors,
    private val types: Map<String, PlaceholderType>,
    private val selected: String?,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val tokens = TemplateParser.tokens(text.text)
        val padded = StringBuilder(text.text)
        tokens.forEach {
            padded[it.start] = NO_BREAK_SPACE
            padded[it.end - 1] = NO_BREAK_SPACE
        }
        val styled =
            buildAnnotatedString {
                append(padded.toString())
                tokens.forEach { addStyle(styleFor(it.key), it.start, it.end) }
            }
        return TransformedText(styled, OffsetMapping.Identity)
    }

    private fun styleFor(key: String): SpanStyle {
        val camera = types[key] == PlaceholderType.Camera
        val active = key == selected
        val (background, content) =
            when {
                camera && active -> colors.personBStrong to colors.onPersonBStrong
                camera -> colors.personB to colors.onPersonB
                active -> colors.accent to colors.onAccent
                else -> colors.accentSoft to colors.accentStrong
            }
        return SpanStyle(background = background, color = content, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MessageField(
    draft: EditorDraft,
    onChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val c = Deaftly.colors
    var focused by remember { mutableStateOf(false) }
    val types = draft.placeholders.associate { it.key to it.type }
    val transformation = remember(c, types, draft.selectedKey) { TokenTransformation(c, types, draft.selectedKey) }
    BasicTextField(
        value = draft.bodyValue,
        onValueChange = onChange,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics { if (error != null) error(error) },
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                fontSize = MessageTextSize,
                lineHeight = MessageLineHeight,
                color = c.ink,
            ),
        cursorBrush = SolidColor(c.accent),
        visualTransformation = transformation,
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8)) {
                FieldLabel(stringResource(R.string.editor_message))
                FieldFrame(
                    focused = focused,
                    invalid = error != null,
                    minHeight = MessageMinHeight,
                    shape = Deaftly.shapes.cardSmall,
                    alignment = Alignment.TopStart,
                ) {
                    if (draft.body.isEmpty()) {
                        Text(
                            text = stringResource(R.string.editor_message_hint),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = MessageTextSize, lineHeight = MessageLineHeight),
                            color = c.inkMuted,
                        )
                    }
                    inner()
                }
                error?.let { FieldError(it) }
            }
        },
    )
}
