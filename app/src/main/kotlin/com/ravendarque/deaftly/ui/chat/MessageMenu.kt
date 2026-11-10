package com.ravendarque.deaftly.ui.chat

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.chat.Message
import com.ravendarque.deaftly.ui.theme.Deaftly
import kotlinx.coroutines.launch

private val MenuBorder = 1.dp

data class MessageActions(
    val onQuote: (messageId: Int) -> Unit,
    val onDelete: (messageId: Int) -> Unit,
)

internal data class MessageCommand(
    val label: String,
    val destructive: Boolean = false,
    val run: () -> Unit,
)

@Composable
internal fun rememberMessageCommands(
    message: Message,
    actions: MessageActions,
): List<MessageCommand> {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copyLabel = stringResource(R.string.chat_copy_action)
    val quoteLabel = stringResource(R.string.chat_quote_action)
    val deleteLabel = stringResource(R.string.chat_delete_action)
    return listOf(
        MessageCommand(copyLabel) {
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, message.plainText))) }
        },
        MessageCommand(quoteLabel) { actions.onQuote(message.id) },
        MessageCommand(deleteLabel, destructive = true) { actions.onDelete(message.id) },
    )
}

@Composable
internal fun MessageMenu(
    expanded: Boolean,
    commands: List<MessageCommand>,
    onDismiss: () -> Unit,
) {
    val c = Deaftly.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, Deaftly.spacing.space8),
        shape = Deaftly.shapes.controlSmall,
        containerColor = c.surface,
        border = BorderStroke(MenuBorder, c.line),
    ) {
        commands.forEach { command ->
            DropdownMenuItem(
                text = { Text(command.label, color = if (command.destructive) c.error else Color.Unspecified) },
                onClick = {
                    onDismiss()
                    command.run()
                },
            )
        }
    }
}
