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
import com.ravendarque.deaftly.ui.ocr.OcrOutcome
import com.ravendarque.deaftly.ui.ocr.OcrOutcomes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val FILL_KEY = "fill"
private const val ACTIVE_KEY = "active"
private const val TYPING_KEY = "typing"

sealed interface FillPart {
    data class Text(
        val text: String,
    ) : FillPart

    data class Filled(
        val placeholder: Placeholder,
        val values: List<String>,
    ) : FillPart

    data class Empty(
        val placeholder: Placeholder,
    ) : FillPart
}

data class ActivePlaceholder(
    val placeholder: Placeholder,
    val values: List<String>,
    val typeInstead: Boolean = false,
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
    private val outcomes: OcrOutcomes = OcrOutcomes(),
) : ViewModel() {
    private val fill = MutableStateFlow(FillCodec.decode(savedState[FILL_KEY]))
    private val activeKey = MutableStateFlow(savedState.get<String>(ACTIVE_KEY))
    private val typingKey = MutableStateFlow(savedState.get<String>(TYPING_KEY))

    private val loaded: StateFlow<Loaded?> =
        repository.templates
            .map { templates -> Loaded(templates.firstOrNull { it.id == templateId }) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<FillUiState> =
        combine(loaded, fill, activeKey, typingKey, ::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FillUiState.Loading)

    init {
        viewModelScope.launch {
            combine(loaded.filterNotNull(), outcomes.pending) { loaded, pending -> loaded.template to pending }
                .collect { (template, pending) ->
                    pending.filter { it.templateId == templateId }.forEach { outcome ->
                        if (template != null) apply(template, outcome)
                        outcomes.consume(outcome)
                    }
                }
        }
    }

    fun activate(key: String) {
        val template = loaded.value?.template ?: return
        if (key in placeholderKeys(template)) setActive(key)
    }

    fun dismiss() {
        setActive(null)
    }

    fun choose(value: String) {
        activeKey.value?.let { fill(it, value) }
    }

    private fun apply(
        template: Template,
        outcome: OcrOutcome,
    ) {
        if (outcome.placeholderKey !in placeholderKeys(template)) return
        when (outcome) {
            is OcrOutcome.Text -> {
                fill(outcome.placeholderKey, outcome.text)
            }

            is OcrOutcome.TypeInstead -> {
                setActive(outcome.placeholderKey)
                typingKey.value = outcome.placeholderKey
                savedState[TYPING_KEY] = outcome.placeholderKey
            }
        }
    }

    private fun fill(
        key: String,
        value: String,
    ) {
        val template = loaded.value?.template
        val clean = value.trim()
        if (template != null && key in placeholderKeys(template) && clean.isNotEmpty()) {
            val wasUnfilled = key in TemplateRenderer.render(template, fill.value).unfilledKeys
            val before = fill.value
            val after =
                when {
                    !template.placeholder(key).repeatable -> before.set(key, clean)
                    clean in before.values(key) -> before
                    else -> before.add(key, clean)
                }
            update(template, after)
            setActive(if (wasUnfilled) TemplateRenderer.render(template, after).unfilledKeys.firstOrNull() else null)
        }
    }

    fun remove(
        key: String,
        index: Int,
    ) {
        val template = loaded.value?.template
        if (template != null && index in fill.value.values(key).indices) update(template, fill.value.removeAt(key, index))
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
        if (typingKey.value != key) {
            typingKey.value = null
            savedState[TYPING_KEY] = null
        }
    }

    private fun buildState(
        loaded: Loaded?,
        fill: TemplateFill,
        activeKey: String?,
        typingKey: String?,
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
                    parts = rendered.parts.map { toPart(template, fill, it) },
                    active =
                        activeKey?.takeIf { it in placeholderKeys(template) }?.let {
                            ActivePlaceholder(template.placeholder(it), fill.values(it), typeInstead = it == typingKey)
                        },
                    text = rendered.text,
                    canShow = rendered.isComplete,
                )
            }
        }
    }

    private fun toPart(
        template: Template,
        fill: TemplateFill,
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
                val placeholder = template.placeholder(part.key)
                val values =
                    if (placeholder.repeatable) {
                        fill.values(part.key).map { it.trim() }.filter { it.isNotEmpty() }
                    } else {
                        listOf(part.text)
                    }
                FillPart.Filled(placeholder, values)
            }
        }

    private class Loaded(
        val template: Template?,
    )
}

private fun placeholderKeys(template: Template): Set<String> =
    template.segments().filterIsInstance<TemplateSegment.PlaceholderRef>().mapTo(linkedSetOf()) { it.key }
