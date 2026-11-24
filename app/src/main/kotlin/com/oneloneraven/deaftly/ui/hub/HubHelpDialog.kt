package com.oneloneraven.deaftly.ui.hub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.ui.components.ButtonMinHeight
import com.oneloneraven.deaftly.ui.components.HubIconDisc
import com.oneloneraven.deaftly.ui.components.PrimaryButton
import com.oneloneraven.deaftly.ui.components.SecondaryButton
import com.oneloneraven.deaftly.ui.icons.DeaftlyIcons
import com.oneloneraven.deaftly.ui.theme.Deaftly

@Composable
internal fun HubHelpDialog(
    title: String,
    help: String,
    icon: ImageVector,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
    onAddToHomeScreen: (() -> Unit)? = null,
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
                if (onAddToHomeScreen != null) {
                    SecondaryButton(
                        stringResource(R.string.hub_help_add_to_home),
                        onAddToHomeScreen,
                        Modifier.fillMaxWidth().padding(top = Deaftly.spacing.space16),
                        DeaftlyIcons.Plus,
                    )
                }
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
