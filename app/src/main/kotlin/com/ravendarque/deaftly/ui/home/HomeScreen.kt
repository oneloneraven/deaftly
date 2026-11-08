package com.ravendarque.deaftly.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.theme.Deaftly

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (Route) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, onNavigate)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onNavigate: (Route) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Deaftly.spacing.space24, vertical = Deaftly.spacing.space20),
            verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space16),
        ) {
            Wordmark()
            state.destinations.forEach { route ->
                Button(
                    onClick = { onNavigate(route) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = Deaftly.shapes.control,
                ) {
                    Text(
                        text = stringResource(route.title),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Wordmark() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.semantics { heading() },
    ) {
        Text(
            text = stringResource(R.string.wordmark),
            style = MaterialTheme.typography.headlineLarge,
        )
        Box(
            Modifier
                .padding(bottom = Deaftly.spacing.space8)
                .size(9.dp)
                .background(Deaftly.colors.accent, CircleShape),
        )
    }
}
