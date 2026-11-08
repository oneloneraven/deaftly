package com.ravendarque.deaftly.ui.chat

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import com.ravendarque.deaftly.core.chat.Composition
import com.ravendarque.deaftly.core.chat.SpanStyle

@Stable
class ComposerState(
    initial: Composition,
    formatting: ChatFormatting,
) {
    var formatting by mutableStateOf(formatting)

    val field = TextFieldState(initial.text, TextRange(initial.selectionStart, initial.selectionEnd))

    var composition by mutableStateOf(initial)
        private set

    private val unechoed = ArrayDeque<Composition>().apply { add(initial) }

    internal fun report(next: Composition): Boolean {
        if (next == unechoed.last()) return false
        unechoed.addLast(next)
        return true
    }

    internal fun accept(external: Composition): Boolean {
        val index = unechoed.indexOf(external)
        if (index >= 0) {
            repeat(index) { unechoed.removeFirst() }
            return false
        }
        unechoed.clear()
        unechoed.addLast(external)
        return true
    }

    val input =
        InputTransformation {
            composition = composition.withText(asCharSequence().toString(), selection.min, selection.max)
        }

    val output =
        OutputTransformation {
            composition.content.ranges.forEach { range ->
                if (range.end <= length) addStyle(formatting.styleOf(range.style), range.start, range.end)
            }
        }

    fun toggle(style: SpanStyle) {
        composition = composition.toggle(style)
    }

    fun follow(selection: TextRange) {
        composition = composition.select(selection.min, selection.max)
    }

    fun replace(next: Composition) {
        composition = next
        field.edit {
            replace(0, length, next.text)
            selection = TextRange(next.selectionStart, next.selectionEnd)
        }
    }
}

@Composable
fun rememberComposerState(
    key: Any,
    draft: Composition,
    onChange: (Composition) -> Unit,
): ComposerState {
    val formatting = rememberChatFormatting()
    val state = remember(key) { ComposerState(draft, formatting) }
    SideEffect { state.formatting = formatting }
    val latestDraft by rememberUpdatedState(draft)
    val latestOnChange by rememberUpdatedState(onChange)
    LaunchedEffect(state) {
        snapshotFlow { state.field.selection }.collect { state.follow(it) }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.composition }.collect {
            if (state.report(it)) latestOnChange(it)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { latestDraft }.collect {
            if (state.accept(it)) state.replace(it)
        }
    }
    return state
}
