package com.ravendarque.deaftly.ui.facetoface

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ravendarque.deaftly.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

private const val MESSAGES_KEY = "messages"
private const val DRAFT_KEY = "draft"

data class FaceToFaceState(
    val messages: List<String> = emptyList(),
    val draft: String = "",
)

class FaceToFaceViewModel(
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val current =
        MutableStateFlow(
            FaceToFaceState(
                messages = savedState.get<ArrayList<String>>(MESSAGES_KEY).orEmpty(),
                draft = savedState.get<String>(DRAFT_KEY).orEmpty(),
            ),
        )

    val state: StateFlow<FaceToFaceState> = current

    fun updateDraft(text: String) {
        current.update { it.copy(draft = text) }
        savedState[DRAFT_KEY] = text
    }

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty()) return
        current.update { it.copy(messages = it.messages + message, draft = "") }
        savedState[MESSAGES_KEY] = ArrayList(current.value.messages)
        savedState[DRAFT_KEY] = ""
    }

    fun continueAsChat(draft: String): Route.Chat = Route.Chat(messages = current.value.messages, draft = draft.trim())
}
