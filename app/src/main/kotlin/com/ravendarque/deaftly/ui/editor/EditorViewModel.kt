package com.ravendarque.deaftly.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.UUID

private const val DRAFT_KEY = "draft"

enum class EditorPhase { Loading, Missing, Ready }

sealed interface EditorExit {
    data object Saved : EditorExit

    data object Cancelled : EditorExit

    data object Deleted : EditorExit

    data class Copied(
        val templateId: String,
    ) : EditorExit
}

class EditorStrings(
    val newPlaceholderName: String,
    val copyTitle: (String) -> String,
)

class EditorViewModel(
    private val templateId: String?,
    private val repository: TemplateRepository,
    private val savedState: SavedStateHandle,
    private val strings: EditorStrings,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    var phase by mutableStateOf(EditorPhase.Loading)
        private set

    var draft by mutableStateOf(EditorDraft())
        private set

    var categories by mutableStateOf<List<String>>(emptyList())
        private set

    var builtIn by mutableStateOf(false)
        private set

    var showErrors by mutableStateOf(false)
        private set

    var exit by mutableStateOf<EditorExit?>(null)
        private set

    private var baseline: Template? by mutableStateOf(null)
    private var busy = false
    private val createdId by lazy(newId)

    val isNew: Boolean get() = templateId == null

    val newPlaceholderName: String get() = strings.newPlaceholderName

    val isDirty: Boolean
        get() = baseline?.let { draft.toTemplate(it.id, it.pinned, it.builtIn) != it } ?: false

    init {
        viewModelScope.launch { load() }
    }

    fun edit(change: (EditorDraft) -> EditorDraft) {
        commit(change(draft))
    }

    fun save() {
        launchGuarded {
            val saved = buildTemplate() ?: return@launchGuarded
            if (templateId == null) repository.add(saved) else repository.update(saved)
            exit = EditorExit.Saved
        }
    }

    fun makeCopy() {
        launchGuarded {
            val copyId = newId()
            val copy = buildTemplate(copyId)?.copy(title = strings.copyTitle(draft.name.trim())) ?: return@launchGuarded
            repository.add(copy.copy(pinned = false, builtIn = false))
            exit = EditorExit.Copied(copyId)
        }
    }

    fun delete() {
        val id = templateId
        if (id == null) return
        launchGuarded {
            repository.delete(id)
            exit = EditorExit.Deleted
        }
    }

    fun restoreOriginal() {
        val id = templateId
        if (id == null || !builtIn) return
        launchGuarded {
            repository.restoreBuiltIn(id)
            repository.templates
                .first()
                .firstOrNull { it.id == id }
                ?.let {
                    val initial = EditorDraft.from(it)
                    baseline = initial.toTemplate(it.id, it.pinned, it.builtIn)
                    commit(initial)
                }
            showErrors = false
        }
    }

    fun discard() {
        exit = EditorExit.Cancelled
    }

    private suspend fun load() {
        val all = repository.templates.first()
        categories = all.map { it.category }.distinct()
        val existing = templateId?.let { id -> all.firstOrNull { it.id == id } }
        if (templateId != null && existing == null) {
            phase = EditorPhase.Missing
            return
        }
        val initial = existing?.let(EditorDraft::from) ?: EditorDraft(category = categories.firstOrNull().orEmpty())
        val id = existing?.id ?: createdId
        baseline = initial.toTemplate(id, existing?.pinned ?: false, existing?.builtIn ?: false)
        builtIn = existing?.builtIn == true
        draft = savedState.get<String>(DRAFT_KEY)?.let(::decodeDraft) ?: initial
        phase = EditorPhase.Ready
    }

    private suspend fun buildTemplate(id: String? = null): Template? {
        val current = baseline
        showErrors = showErrors || draft.errors.isNotEmpty()
        return if (current == null || draft.errors.isNotEmpty()) {
            null
        } else {
            val target = id ?: current.id
            val latest = if (id == null) repository.templates.first().firstOrNull { it.id == target } else null
            draft.toTemplate(target, latest?.pinned ?: current.pinned, current.builtIn)
        }
    }

    private fun commit(next: EditorDraft) {
        draft = next
        savedState[DRAFT_KEY] = Json.encodeToString(EditorDraft.serializer(), next)
    }

    private fun launchGuarded(block: suspend () -> Unit) {
        if (busy || phase != EditorPhase.Ready) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }
}

private fun decodeDraft(encoded: String): EditorDraft? =
    try {
        Json.decodeFromString(EditorDraft.serializer(), encoded)
    } catch (_: SerializationException) {
        null
    }
