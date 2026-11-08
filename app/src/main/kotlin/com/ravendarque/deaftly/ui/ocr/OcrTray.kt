package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val MAX_LINES = 3

@Composable
fun OcrTray(
    hint: String,
    fieldLabel: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Deaftly.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(c.surface, Deaftly.shapes.sheet)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(
                    start = Deaftly.spacing.space20,
                    top = Deaftly.spacing.space16,
                    end = Deaftly.spacing.space20,
                    bottom = Deaftly.spacing.space12,
                ),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
    ) {
        Text(text = hint, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(fieldLabel) },
            trailingIcon = { Icon(DeaftlyIcons.Edit, contentDescription = null) },
            maxLines = MAX_LINES,
            textStyle = MaterialTheme.typography.titleMedium,
            shape = Deaftly.shapes.control,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = c.ink,
                    unfocusedTextColor = c.ink,
                    focusedContainerColor = c.surface,
                    unfocusedContainerColor = c.surface,
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.outline,
                    focusedLabelColor = c.accentStrong,
                    unfocusedLabelColor = c.inkMuted,
                    focusedTrailingIconColor = c.inkMuted,
                    unfocusedTrailingIconColor = c.inkMuted,
                    cursorColor = c.accent,
                ),
        )
    }
}
