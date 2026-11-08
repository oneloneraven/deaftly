package com.ravendarque.deaftly.ui.firstrun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.chat.ChatProfile
import com.ravendarque.deaftly.core.chat.ColourRole
import com.ravendarque.deaftly.core.settings.ChatLayout
import com.ravendarque.deaftly.core.settings.SettingsRepository
import com.ravendarque.deaftly.ui.chat.PARTICIPANT_NAME_MAX_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FirstRunState(
    val name: String = "",
    val colour: ColourRole = ColourRole.Accent,
    val layout: ChatLayout = ChatLayout.Sides,
)

class FirstRunViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    private val draft = MutableStateFlow(FirstRunState())
    val state: StateFlow<FirstRunState> = draft.asStateFlow()

    fun setName(name: String) = draft.update { it.copy(name = name.take(PARTICIPANT_NAME_MAX_LENGTH)) }

    fun setColour(colour: ColourRole) = draft.update { it.copy(colour = colour) }

    fun setLayout(layout: ChatLayout) = draft.update { it.copy(layout = layout) }

    fun getStarted(onDone: () -> Unit) {
        val chosen = draft.value
        viewModelScope.launch {
            settings.setChatProfile(ChatProfile(chosen.name.trim().takeIf { it.isNotEmpty() }, chosen.colour))
            settings.setChatLayout(chosen.layout)
            settings.setFirstRunDone()
            onDone()
        }
    }
}
