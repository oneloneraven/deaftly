package com.ravendarque.deaftly.ui.placeholder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit,
    body: String? = null,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(Deaftly.spacing.space24)) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (!body.isNullOrBlank()) {
                Text(text = body, style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}
