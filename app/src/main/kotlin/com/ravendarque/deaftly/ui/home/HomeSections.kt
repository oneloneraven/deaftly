package com.ravendarque.deaftly.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.RenderedPart
import com.ravendarque.deaftly.navigation.Route
import com.ravendarque.deaftly.ui.components.CategoryChip
import com.ravendarque.deaftly.ui.components.ListCard
import com.ravendarque.deaftly.ui.components.PinnedTile
import com.ravendarque.deaftly.ui.components.PreviewPart
import com.ravendarque.deaftly.ui.components.SecondaryButton
import com.ravendarque.deaftly.ui.components.TileTone
import com.ravendarque.deaftly.ui.icons.DeaftlyIcons
import com.ravendarque.deaftly.ui.theme.Deaftly

private const val TILES_PER_ROW = 2
private const val SINGLE_COLUMN_FONT_SCALE = 1.5f
private const val UNFILLED_ELLIPSIS = "\u2026"
private val tileIcons = listOf(DeaftlyIcons.Home, DeaftlyIcons.Utensils)

@Composable
internal fun TemplateList(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                start = Deaftly.spacing.space20,
                top = Deaftly.spacing.space12,
                end = Deaftly.spacing.space20,
                bottom = Deaftly.spacing.space12,
            ),
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space10),
    ) {
        if (state.pinned.isNotEmpty()) {
            item(key = "pinned") {
                PinnedSection(state.pinned, actions.onNavigate, Modifier.padding(bottom = Deaftly.spacing.space12))
            }
        }
        if (state.categories.isNotEmpty()) {
            item(key = "categories") {
                CategoryRow(state, actions.onCategorySelected, Modifier.padding(bottom = Deaftly.spacing.space4))
            }
        }
        if (!state.isLoading && state.templates.isEmpty()) {
            item(key = "empty") { EmptyState(state.query, actions.onClearSearch) }
        }
        items(state.templates, key = { it.id }) { template ->
            ListCard(
                title = template.title,
                preview = template.preview.map(::toPreviewPart),
                onClick = { actions.onNavigate(Route.Fill(template.id)) },
            )
        }
    }
}

@Composable
private fun PinnedSection(
    pinned: List<HomeTemplate>,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12)) {
        Text(
            text = stringResource(R.string.home_pinned).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Deaftly.colors.inkMuted,
            modifier = Modifier.semantics { heading() },
        )
        val perRow = if (LocalDensity.current.fontScale >= SINGLE_COLUMN_FONT_SCALE) 1 else TILES_PER_ROW
        pinned.withIndex().chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space12),
            ) {
                row.forEach { (index, template) ->
                    PinnedTile(
                        title = template.title,
                        preview = tilePreview(template.preview),
                        icon = tileIcons[index % tileIcons.size],
                        onClick = { onNavigate(Route.Fill(template.id)) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        tone = if (index % 2 == 0) TileTone.Accent else TileTone.PersonB,
                    )
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    state: HomeUiState,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.home_categories)
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .selectableGroup()
                .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Deaftly.spacing.space8),
    ) {
        CategoryChip(
            label = stringResource(R.string.home_category_all),
            selected = state.selectedCategory == null,
            onClick = { onCategorySelected(null) },
        )
        state.categories.forEach { category ->
            CategoryChip(
                label = category,
                selected = state.selectedCategory == category,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun EmptyState(
    query: String,
    onClearSearch: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Deaftly.spacing.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Deaftly.spacing.space16),
    ) {
        if (query.isBlank()) {
            Text(
                text = stringResource(R.string.home_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = Deaftly.colors.inkSecondary,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(R.string.home_no_results, query.trim()),
                style = MaterialTheme.typography.bodyLarge,
                color = Deaftly.colors.inkSecondary,
                textAlign = TextAlign.Center,
            )
            SecondaryButton(stringResource(R.string.home_clear_search), onClick = onClearSearch)
        }
    }
}

private fun toPreviewPart(part: RenderedPart): PreviewPart =
    when (part) {
        is RenderedPart.Text -> PreviewPart.Plain(part.text)
        is RenderedPart.Value -> PreviewPart.Plain(part.text)
        is RenderedPart.Unfilled -> PreviewPart.Placeholder(part.label)
    }

private fun tilePreview(parts: List<RenderedPart>): String =
    parts.joinToString("") {
        when (it) {
            is RenderedPart.Text -> it.text
            is RenderedPart.Value -> it.text
            is RenderedPart.Unfilled -> UNFILLED_ELLIPSIS
        }
    }
