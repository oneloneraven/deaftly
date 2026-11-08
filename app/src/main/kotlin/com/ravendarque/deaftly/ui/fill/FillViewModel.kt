package com.ravendarque.deaftly.ui.fill

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.RenderedPart
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateFill
import com.ravendarque.deaftly.core.template.TemplateRenderer
import com.ravendarque.deaftly.core.template.TemplateRepository
import com.ravendarque.deaftly.core.template.TemplateSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val FILL_KEY = "fill"
private const val ACTIVE_KEY = "active"

sealed interface FillPart {
    data class Text(
        val text: String,
    ) : FillPart

    data class Filled(
        val placeholder: Placeholder,
        val text: String,
    ) : FillPart

    data class Empty(
        val placeholder: Placeholder,
    ) : FillPart
}

data class ActivePlaceholder(
    val placeholder: Placeholder,
    val value: String?,
)

sealed interface FillUiState {
    data object Loading : FillUiState

    data object Missing : FillUiState

    data class Ready(
        val title: String,
        val parts: List<FillPart>,
        val active: ActivePlaceholder?,
        val text: String,
        val canShow: Boolean,
    ) : FillUiState
}

class FillViewModel(
    private val templateId: String,
    repository: TemplateRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val fill = MutableStateFlow(FillCodec.decode(savedState[FILL_KEY]))
    private val activeKey = MutableStateFlow(savedState.get<String>(ACTIVE_KEY))

    private val loaded: StateFlow<Loaded?> =
        repository.templates
            .map { templates -> Loaded(templates.firstOrNull { it.id == templateId }) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<FillUiState> =
        combine(loaded, fill, activeKey, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FillUiState.Loading)

    fun activate(key: String) {
        val template = loaded.value?.template ?: return
        if (key in placeholderKeys(template)) setActive(key)
    }

    fun dismiss() {
        setActive(null)
    }

    fun choose(value: String) {
        val template = loaded.value?.template
        val key = activeKey.value
        val clean = value.trim()
        if (template != null && key != null && clean.isNotEmpty()) {
            val wasUnfilled = key in TemplateRenderer.render(template, fill.value).unfilledKeys
            val after = fill.value.set(key, clean)
            update(template, after)
            setActive(if (wasUnfilled) TemplateRenderer.render(template, after).unfilledKeys.firstOrNull() else null)
        }
    }

    private fun update(
        template: Template,
        value: TemplateFill,
    ) {
        fill.value = value
        savedState[FILL_KEY] = FillCodec.encode(placeholderKeys(template), value)
    }

    private fun setActive(key: String?) {
        activeKey.value = key
        savedState[ACTIVE_KEY] = key
    }

    private fun buildState(
        loaded: Loaded?,
        fill: TemplateFill,
        activeKey: String?,
    ): FillUiState {
        val template = loaded?.template
        return when {
            loaded == null -> {
                FillUiState.Loading
            }

            template == null -> {
                FillUiState.Missing
            }

            else -> {
                val rendered = TemplateRenderer.render(template, fill)
                FillUiState.Ready(
                    title = template.title,
                    parts = rendered.parts.map { toPart(template, it) },
                    active =
                        activeKey?.takeIf { it in placeholderKeys(template) }?.let {
                            ActivePlaceholder(template.placeholder(it), fill.value(it))
                        },
                    text = rendered.text,
                    canShow = rendered.isComplete,
                )
            }
        }
    }

    private fun toPart(
        template: Template,
        part: RenderedPart,
    ): FillPart =
        when (part) {
            is RenderedPart.Text -> {
                FillPart.Text(part.text)
            }

            is RenderedPart.Unfilled -> {
                FillPart.Empty(template.placeholder(part.key))
            }

            is RenderedPart.Value -> {
                FillPart.Filled(template.placeholder(part.key), part.text)
            }
        }

    private fun placeholderKeys(template: Template): Set<String> =
        template.segments().filterIsInstance<TemplateSegment.PlaceholderRef>().mapTo(linkedSetOf()) { it.key }

    private class Loaded(
        val template: Template?,
    )
}
