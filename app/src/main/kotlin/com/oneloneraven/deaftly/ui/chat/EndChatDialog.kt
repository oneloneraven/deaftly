package com.oneloneraven.deaftly.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.core.chat.Conversation
import com.oneloneraven.deaftly.ui.components.PrimaryButton
import com.oneloneraven.deaftly.ui.theme.Deaftly
import kotlinx.coroutines.launch

private val TargetSize = 48.dp

@Composable
fun EndChatDialog(
    saveFailed: Boolean,
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
                if (saveFailed) {
                    Text(
                        stringResource(R.string.chat_keep_failed),
                        color = c.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
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

@Composable
fun KeepChatDialog(
    conversation: Conversation,
    keeper: ChatKeeper,
    onEnd: () -> Unit,
    onKeepChatting: () -> Unit,
) {
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    EndChatDialog(
        saveFailed = saveFailed,
        onKeep = { scope.launch { if (keeper.keep(conversation)) onEnd() else saveFailed = true } },
        onDelete = onEnd,
        onKeepChatting = onKeepChatting,
    )
}
