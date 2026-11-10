package com.ravendarque.deaftly.ui.hub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.components.ButtonMinHeight
import com.ravendarque.deaftly.ui.components.HubIconDisc
import com.ravendarque.deaftly.ui.components.PrimaryButton
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
internal fun HubHelpDialog(
    title: String,
    help: String,
    icon: ImageVector,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Deaftly.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        iconContentColor = c.accent,
        titleContentColor = c.ink,
        textContentColor = c.inkSecondary,
        icon = { HubIconDisc(icon) },
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(help, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = { PrimaryButton(stringResource(R.string.hub_help_start), onClick = onStart) },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = ButtonMinHeight)) {
                Text(stringResource(R.string.hub_help_close), color = c.ink)
            }
        },
    )
}
