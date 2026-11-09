package com.ravendarque.deaftly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.semantics { heading() },
    ) {
        Text(
            text = stringResource(R.string.wordmark),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.spoken(stringResource(R.string.wordmark)),
        )
        Box(
            Modifier
                .padding(bottom = Deaftly.spacing.space8)
                .size(9.dp)
                .background(Deaftly.colors.accent, CircleShape),
        )
    }
}
