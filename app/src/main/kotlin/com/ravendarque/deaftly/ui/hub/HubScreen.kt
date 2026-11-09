package com.ravendarque.deaftly.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.HubCard
import com.ravendarque.deaftly.ui.components.Wordmark
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val STACK_FONT_SCALE = 1.5f

private data class HubOption(
    val label: Int,
    val icon: ImageVector,
    val route: Route,
)

private val showOptions =
    listOf(
        HubOption(R.string.hub_start_typing, DeaftlyIcons.Type, Route.TypeShow),
        HubOption(R.string.hub_use_template, DeaftlyIcons.Template, Route.Templates),
    )

private val chatOptions =
    listOf(
        HubOption(R.string.hub_face_to_face, DeaftlyIcons.FaceToFace, Route.FaceToFace),
        HubOption(R.string.hub_side_by_side, DeaftlyIcons.Sides, Route.Chat()),
    )

@Composable
fun HubScreen(onNavigate: (Route) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Header(onSettings = { onNavigate(Route.Settings) })
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Deaftly.spacing.space20, vertical = Deaftly.spacing.space12),
                verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space24),
            ) {
                Group(R.string.hub_show_heading, showOptions, onNavigate)
                Group(R.string.hub_chat_heading, chatOptions, onNavigate)
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = Deaftly.spacing.space24,
                    top = Deaftly.spacing.space12,
                    end = Deaftly.spacing.space20,
                    bottom = Deaftly.spacing.space12,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(Modifier.weight(1f))
        DeaftlyIconButton(DeaftlyIcons.Settings, stringResource(R.string.route_settings), onClick = onSettings)
    }
}

@Composable
private fun Group(
    heading: Int,
    options: List<HubOption>,
    onNavigate: (Route) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12)) {
        Text(
            text = stringResource(heading),
            style = MaterialTheme.typography.titleLarge,
            color = Deaftly.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        val perRow = if (LocalDensity.current.fontScale >= STACK_FONT_SCALE) 1 else options.size
        options.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
            ) {
                row.forEach { option ->
                    HubCard(
                        label = stringResource(option.label),
                        icon = option.icon,
                        onClick = { onNavigate(option.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
