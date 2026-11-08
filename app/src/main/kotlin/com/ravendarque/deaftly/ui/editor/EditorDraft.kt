package com.ravendarque.deaftly.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template
import com.ravendarque.deaftly.core.template.TemplateEditing
import kotlinx.serialization.Serializable

enum class KeyError { Blank, Braces, Taken }

enum class EditorError { NameBlank, CategoryBlank, MessageBlank, DuplicateKey }

@Serializable
data class DraftPlaceholder(
    val key: String,
    val label: String = "",
    val type: PlaceholderType = PlaceholderType.Typing,
    val options: List<String> = emptyList(),
    val repeatable: Boolean = false,
) {
    fun toPlaceholder(): Placeholder =
        Placeholder(
            key = key,
            label = label.trim().ifBlank { key },
            type = type,
            options =
                if (type == PlaceholderType.Choices) {
                    options.map(String::trim).filter(String::isNotEmpty).distinct()
                } else {
                    emptyList()
                },
            repeatable = repeatable,
        )

    companion object {
        fun from(placeholder: Placeholder) =
            DraftPlaceholder(
                key = placeholder.key,
                label = placeholder.label.takeUnless { it == placeholder.key }.orEmpty(),
                type = placeholder.type,
                options = placeholder.options,
                repeatable = placeholder.repeatable,
            )
    }
}

@Serializable
data class EditorDraft(
    val name: String = "",
    val category: String = "",
    val body: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val placeholders: List<DraftPlaceholder> = emptyList(),
    val selectedKey: String? = null,
    val keyInput: String = "",
    val selectionVersion: Int = 0,
) {
    val bodyValue: TextFieldValue
        get() = TextFieldValue(body, TextRange(selectionStart.coerceAtMost(body.length), selectionEnd.coerceAtMost(body.length)))

    val selected: DraftPlaceholder?
        get() = placeholders.firstOrNull { it.key == selectedKey }

    val keyError: KeyError?
        get() {
            val current = selectedKey ?: return null
            val candidate = keyInput.trim()
            val taken = (placeholders.map { it.key } + TemplateEditing.keys(body)).toSet() - current
            return when {
                candidate.isBlank() -> KeyError.Blank
                '{' in candidate || '}' in candidate -> KeyError.Braces
                candidate in taken -> KeyError.Taken
                else -> null
            }
        }

    val errors: Set<EditorError>
        get() =
            buildSet {
                if (name.isBlank()) add(EditorError.NameBlank)
                if (category.isBlank()) add(EditorError.CategoryBlank)
                if (body.isBlank()) add(EditorError.MessageBlank)
                if (placeholders.map { it.key }.let { it.size != it.toSet().size }) add(EditorError.DuplicateKey)
            }

    fun toTemplate(
        id: String,
        pinned: Boolean = false,
        builtIn: Boolean = false,
    ): Template {
        val defined = placeholders.associateBy { it.key }
        val used =
            TemplateEditing.keys(body).map { key ->
                defined[key]?.toPlaceholder() ?: Placeholder(key, key, PlaceholderType.Typing)
            }
        return Template(
            id = id,
            title = name.trim(),
            category = category.trim(),
            body = body,
            pinned = pinned,
            builtIn = builtIn,
            placeholders = used.associateBy { it.key },
        )
    }

    companion object {
        fun from(template: Template) =
            EditorDraft(
                name = template.title,
                category = template.category,
                body = template.body,
                selectionStart = template.body.length,
                selectionEnd = template.body.length,
                placeholders = template.placeholders.values.map(DraftPlaceholder::from),
            )
    }
}
