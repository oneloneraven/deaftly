package com.ravendarque.deaftly.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.ravendarque.deaftly.core.template.TemplateEditing

fun EditorDraft.withBody(value: TextFieldValue): EditorDraft {
    val kept = TemplateEditing.keepTokensWhole(body, value.text)
    val text = kept?.text ?: value.text
    val start = kept?.cursor ?: value.selection.start
    val end = kept?.cursor ?: value.selection.end
    val edited = copy(body = text, selectionStart = start, selectionEnd = end).dropMissingSelection()
    val token = if (start == end) TemplateEditing.tokenAt(text, start) else null
    return token?.let { edited.select(it.key) } ?: edited
}

fun EditorDraft.insertPlaceholder(defaultName: String): EditorDraft {
    val key = TemplateEditing.nextFreeKey(defaultName, placeholders.map { it.key } + TemplateEditing.keys(body))
    val edit = TemplateEditing.insertToken(body, selectionStart, selectionEnd, key)
    return copy(
        body = edit.text,
        selectionStart = edit.cursor,
        selectionEnd = edit.cursor,
        placeholders = placeholders + DraftPlaceholder(key),
        selectedKey = key,
        keyInput = key,
        selectionVersion = selectionVersion + 1,
    ).dropMissingSelection()
}

fun EditorDraft.select(key: String): EditorDraft =
    when {
        key == selectedKey || key !in TemplateEditing.keys(body) -> {
            this
        }

        else -> {
            copy(
                placeholders = if (placeholders.any { it.key == key }) placeholders else placeholders + DraftPlaceholder(key),
                selectedKey = key,
                keyInput = key,
                selectionVersion = selectionVersion + 1,
            )
        }
    }

fun EditorDraft.renameSelected(input: String): EditorDraft {
    val current = selectedKey
    val typed = copy(keyInput = input)
    val candidate = input.trim()
    return if (current == null || typed.keyError != null || candidate == current) typed else typed.renamed(current, candidate)
}

private fun EditorDraft.renamed(
    current: String,
    candidate: String,
): EditorDraft {
    val rename = TemplateEditing.renameKey(body, current, candidate)
    return copy(
        body = rename.text,
        selectionStart = rename.mapOffset(selectionStart),
        selectionEnd = rename.mapOffset(selectionEnd),
        placeholders = placeholders.map { if (it.key == current) it.copy(key = candidate) else it },
        selectedKey = candidate,
    )
}

fun EditorDraft.updateSelected(transform: (DraftPlaceholder) -> DraftPlaceholder): EditorDraft {
    val current = selectedKey ?: return this
    return copy(placeholders = placeholders.map { if (it.key == current) transform(it) else it })
}

fun EditorDraft.deleteSelected(): EditorDraft {
    val current = selectedKey ?: return this
    val text = TemplateEditing.removeKey(body, current)
    return copy(
        body = text,
        selectionStart = selectionStart.coerceAtMost(text.length),
        selectionEnd = selectionEnd.coerceAtMost(text.length),
        placeholders = placeholders.filterNot { it.key == current },
        selectedKey = null,
        keyInput = "",
        selectionVersion = selectionVersion + 1,
    )
}

private fun EditorDraft.dropMissingSelection(): EditorDraft =
    if (selectedKey != null && selectedKey !in TemplateEditing.keys(body)) {
        copy(selectedKey = null, keyInput = "", selectionVersion = selectionVersion + 1)
    } else {
        this
    }
