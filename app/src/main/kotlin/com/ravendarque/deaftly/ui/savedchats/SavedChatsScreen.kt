package com.ravendarque.deaftly.ui.savedchats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.storage.SavedChatSummary
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.DeaftlyTopBar
import com.ravendarque.deaftly.ui.editor.ConfirmDialog
import com.ravendarque.deaftly.ui.editor.ConfirmSpec
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val RowMinHeight = 72.dp
private val DateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
fun SavedChatsScreen(
    viewModel: SavedChatsViewModel,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(title = stringResource(R.string.hub_saved_chats), onNavigate = onBack)
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space8),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
            ) {
                if (chats.isEmpty()) {
                    Text(
                        text = stringResource(R.string.saved_chats_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Deaftly.colors.inkSecondary,
                    )
                }
                chats.forEach { chat ->
                    SavedChatRow(chat, onOpen = { onOpen(chat.id) }, onDelete = { deleting = chat.id })
                }
            }
        }
    }
    deleting?.let { id ->
        ConfirmDialog(
            spec =
                ConfirmSpec(
                    title = stringResource(R.string.saved_chats_delete_title),
                    text = stringResource(R.string.saved_chats_delete_text),
                    confirm = stringResource(R.string.saved_chats_delete_confirm),
                    dismiss = stringResource(R.string.saved_chats_delete_cancel),
                    destructive = true,
                ),
            onConfirm = {
                deleting = null
                viewModel.delete(id)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SavedChatRow(
    chat: SavedChatSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = Deaftly.shapes.cardSmall
    val name = chat.otherName ?: stringResource(R.string.saved_chats_them)
    val date = DateFormat.format(Instant.ofEpochMilli(chat.changedAt).atZone(ZoneId.systemDefault()))
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .clip(shape)
                .background(Deaftly.colors.surface, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(horizontal = Deaftly.spacing.space16, vertical = Deaftly.spacing.space12),
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space4),
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, color = Deaftly.colors.ink)
            Text(
                text = chat.lastMessage ?: stringResource(R.string.saved_chats_no_messages),
                style = MaterialTheme.typography.bodyMedium,
                color = Deaftly.colors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = date, style = MaterialTheme.typography.bodySmall, color = Deaftly.colors.inkSecondary)
        }
        DeaftlyIconButton(DeaftlyIcons.Trash, stringResource(R.string.saved_chats_delete, name), onClick = onDelete)
    }
}
