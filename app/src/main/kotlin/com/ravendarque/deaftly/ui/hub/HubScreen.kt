package com.ravendarque.deaftly.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.DeaftlyIconButton
import com.ravendarque.deaftly.ui.components.HubCard
import com.ravendarque.deaftly.ui.components.Wordmark
import com.ravendarque.deaftly.ui.components.spoken
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val STACK_FONT_SCALE = 1.5f

private data class HubOption(
    val label: Int,
    val help: Int,
    val icon: ImageVector,
    val route: Route,
)

private val showOptions =
    listOf(
        HubOption(R.string.hub_start_typing, R.string.hub_start_typing_help, DeaftlyIcons.Keyboard, Route.TypeShow),
        HubOption(R.string.hub_use_template, R.string.hub_use_template_help, DeaftlyIcons.TemplateBlank, Route.Templates),
    )

private val chatOptions =
    listOf(
        HubOption(R.string.hub_face_to_face, R.string.hub_face_to_face_help, DeaftlyIcons.FaceToFace, Route.FaceToFace),
        HubOption(R.string.hub_side_by_side, R.string.hub_side_by_side_help, DeaftlyIcons.SideBySide, Route.Chat()),
    )

private val supportOptions =
    listOf(
        HubOption(R.string.hub_help, R.string.hub_help_help, DeaftlyIcons.Help, Route.Help),
        HubOption(R.string.hub_share, R.string.hub_share_help, DeaftlyIcons.Share, Route.Share),
    )

private val allOptions = showOptions + chatOptions + supportOptions

@Composable
fun HubScreen(onNavigate: (Route) -> Unit) {
    var helpLabel by rememberSaveable { mutableStateOf<Int?>(null) }
    val helpOption = allOptions.firstOrNull { it.label == helpLabel }
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
                Text(
                    text = stringResource(R.string.hub_welcome),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Deaftly.colors.inkSecondary,
                    modifier = Modifier.spoken(stringResource(R.string.hub_welcome)),
                )
                EqualCardHeights(
                    perRow = cardsPerRow(),
                    options = allOptions,
                ) { cardHeight ->
                    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space24)) {
                        Group(R.string.hub_show_heading, showOptions, cardHeight, onNavigate) { helpLabel = it.label }
                        Group(R.string.hub_chat_heading, chatOptions, cardHeight, onNavigate) { helpLabel = it.label }
                        Group(R.string.hub_support_heading, supportOptions, cardHeight, onNavigate) { helpLabel = it.label }
                    }
                }
            }
        }
    }
    helpOption?.let { option ->
        HubHelpDialog(
            title = stringResource(option.label),
            help = stringResource(option.help),
            icon = option.icon,
            onStart = {
                helpLabel = null
                onNavigate(option.route)
            },
            onDismiss = { helpLabel = null },
        )
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
private fun cardsPerRow() = if (LocalDensity.current.fontScale >= STACK_FONT_SCALE) 1 else 2

@Composable
private fun EqualCardHeights(
    perRow: Int,
    options: List<HubOption>,
    content: @Composable (cardHeight: Dp) -> Unit,
) {
    val gap = Deaftly.spacing.space12
    SubcomposeLayout { constraints ->
        val gapPx = gap.roundToPx()
        val cardWidth = (constraints.maxWidth - gapPx * (perRow - 1)) / perRow
        val tallest =
            subcompose(ProbeSlot) { options.forEach { HubOptionCard(it, Modifier.clearAndSetSemantics {}) } }
                .maxOf { it.measure(Constraints.fixedWidth(cardWidth)).height }
        val body = subcompose(BodySlot) { content(tallest.toDp()) }.map { it.measure(constraints) }
        layout(body.maxOf { it.width }, body.sumOf { it.height }) {
            var y = 0
            body.forEach {
                it.place(0, y)
                y += it.height
            }
        }
    }
}

private data object ProbeSlot

private data object BodySlot

@Composable
private fun Group(
    heading: Int,
    options: List<HubOption>,
    cardHeight: Dp,
    onNavigate: (Route) -> Unit,
    onHelp: (HubOption) -> Unit,
) {
    val perRow = cardsPerRow()
    Column(verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12)) {
        Text(
            text = stringResource(heading),
            style = MaterialTheme.typography.titleLarge,
            color = Deaftly.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        options.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12)) {
                row.forEach { option ->
                    HubOptionCard(
                        option = option,
                        modifier = Modifier.weight(1f),
                        minHeight = cardHeight,
                        onClick = { onNavigate(option.route) },
                        onInfo = { onHelp(option) },
                    )
                }
                if (row.size < perRow) Spacer(Modifier.weight((perRow - row.size).toFloat()))
            }
        }
    }
}

@Composable
private fun HubOptionCard(
    option: HubOption,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dp.Unspecified,
    onClick: () -> Unit = {},
    onInfo: () -> Unit = {},
) {
    val title = stringResource(option.label)
    HubCard(
        title = title,
        icon = option.icon,
        onClick = onClick,
        infoLabel = stringResource(R.string.hub_info, title),
        onInfo = onInfo,
        modifier = modifier,
        minHeight = minHeight,
    )
}
