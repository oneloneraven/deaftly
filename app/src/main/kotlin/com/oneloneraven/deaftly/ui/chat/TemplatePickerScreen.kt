package com.oneloneraven.deaftly.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oneloneraven.deaftly.R
import com.oneloneraven.deaftly.core.template.RenderedPart
import com.oneloneraven.deaftly.navigation.Route
import com.oneloneraven.deaftly.ui.components.DeaftlyTopBar
import com.oneloneraven.deaftly.ui.home.HomeActions
import com.oneloneraven.deaftly.ui.home.HomeTemplate
import com.oneloneraven.deaftly.ui.home.HomeUiState
import com.oneloneraven.deaftly.ui.home.HomeViewModel
import com.oneloneraven.deaftly.ui.home.SearchField
import com.oneloneraven.deaftly.ui.home.TemplateList

@Composable
fun TemplatePickerScreen(
    viewModel: HomeViewModel,
    onFill: (templateId: String) -> Unit,
    onInsert: (text: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel, onFill, onInsert) {
            HomeActions(
                onNavigate = { route ->
                    if (route is Route.Fill) {
                        val ready = viewModel.state.value.readyText(route.templateId)
                        if (ready != null) onInsert(ready) else onFill(route.templateId)
                    }
                },
                onQueryChange = viewModel::onQueryChange,
                onClearSearch = viewModel::onClearSearch,
                onCategorySelected = viewModel::onCategorySelected,
                onSetPinned = viewModel::setPinned,
                onSetCategory = viewModel::setCategory,
                onDelete = viewModel::delete,
            )
        }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            DeaftlyTopBar(title = stringResource(R.string.chat_insert_template), onNavigate = onBack)
            SearchField(state.query, actions.onQueryChange)
            TemplateList(state, actions, onOpenOptions = null, modifier = Modifier.weight(1f))
        }
    }
}

private fun HomeUiState.readyText(templateId: String): String? =
    (pinned + templates)
        .firstOrNull { it.id == templateId }
        ?.takeIf(HomeTemplate::hasNoPlaceholders)
        ?.preview
        ?.joinToString("") { (it as RenderedPart.Text).text }

private fun HomeTemplate.hasNoPlaceholders() = preview.all { it is RenderedPart.Text }
