package com.oneloneraven.deaftly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oneloneraven.deaftly.core.template.RenderedPart
import com.oneloneraven.deaftly.core.template.Template
import com.oneloneraven.deaftly.core.template.TemplateFill
import com.oneloneraven.deaftly.core.template.TemplateRenderer
import com.oneloneraven.deaftly.core.template.TemplateRepository
import com.oneloneraven.deaftly.core.template.TemplateSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

data class HomeTemplate(
    val id: String,
    val title: String,
    val category: String,
    val pinned: Boolean,
    val preview: List<RenderedPart>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val pinned: List<HomeTemplate> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val query: String = "",
    val templates: List<HomeTemplate> = emptyList(),
)

class HomeViewModel(
    private val repository: TemplateRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeUiState> =
        combine(repository.templates, query, selectedCategory, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onClearSearch() {
        query.value = ""
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.value = category
    }

    fun setPinned(
        id: String,
        pinned: Boolean,
    ) {
        viewModelScope.launch { repository.setPinned(id, pinned) }
    }

    fun setCategory(
        id: String,
        category: String,
    ) {
        val name = category.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            val existing =
                repository.templates
                    .first()
                    .map { it.category }
                    .firstOrNull { it.equals(name, ignoreCase = true) }
            repository.setCategory(id, existing ?: name)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    private fun buildState(
        templates: List<Template>,
        query: String,
        selected: String?,
    ): HomeUiState {
        val categories = templates.map { it.category }.distinct()
        val category = selected?.takeIf { it in categories }
        val (pinned, others) =
            templates
                .filter { TemplateSearch.matches(it, query) && (category == null || it.category == category) }
                .partition { it.pinned }
        return HomeUiState(
            isLoading = false,
            pinned = pinned.map(::toHomeTemplate),
            categories = categories,
            selectedCategory = category,
            query = query,
            templates = others.map(::toHomeTemplate),
        )
    }

    private fun toHomeTemplate(template: Template) =
        HomeTemplate(
            template.id,
            template.title,
            template.category,
            template.pinned,
            TemplateRenderer.render(template, TemplateFill()).parts,
        )
}
