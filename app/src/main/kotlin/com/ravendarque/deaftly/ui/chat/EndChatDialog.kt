package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.theme.Deaftly

private val TargetSize = 48.dp

@Composable
fun EndChatDialog(
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    onKeepChatting: () -> Unit,
) {
    val c = Deaftly.colors
    AlertDialog(
        onDismissRequest = onKeepChatting,
        containerColor = c.surface,
        titleContentColor = c.ink,
        textContentColor = c.inkSecondary,
        title = { Text(stringResource(R.string.chat_keep_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10)) {
                Text(stringResource(R.string.chat_keep_text))
                PrimaryButton(text = stringResource(R.string.chat_keep_confirm), onClick = onKeep, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = TargetSize)) {
                    Text(stringResource(R.string.chat_keep_delete), color = c.error, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onKeepChatting, modifier = Modifier.fillMaxWidth().heightIn(min = TargetSize)) {
                    Text(stringResource(R.string.chat_end_keep), color = c.ink)
                }
            }
        },
        confirmButton = {},
    )
}
