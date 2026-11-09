package com.ravendarque.deaftly.ui.ocr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.LiveRecognition
import com.ravendarque.deaftly.core.ocr.OcrSelection
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.PhraseSelection
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.TextRecogniser
import com.ravendarque.deaftly.core.ocr.TextSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class OcrUiState(
    val ready: Boolean = false,
    val phrases: List<Phrase> = emptyList(),
    val selection: OcrSelection = OcrSelection(),
    val fieldText: String = "",
    val fieldLabel: String? = null,
    val freezeRequested: Boolean = false,
    val frozen: Boolean = false,
    val frozenImage: ImageBitmap? = null,
    val torchAvailable: Boolean = false,
    val torchOn: Boolean = false,
)

class OcrViewModel(
    private val createRecogniser: () -> TextRecogniser,
    private val captureFrame: (RecognitionFrame, BoundingBox?) -> ImageBitmap? = { _, _ -> null },
    private val loadFieldLabel: suspend () -> String? = { null },
) : ViewModel() {
    private val lock = Any()
    private var recogniser: TextRecogniser? = null
    private var live: LiveRecognition? = null
    private var cleared = false
    private val mutableState = MutableStateFlow(OcrUiState())

    val state: StateFlow<OcrUiState> = mutableState

    init {
        viewModelScope.launch { mutableState.update { it.copy(fieldLabel = loadFieldLabel()) } }
        viewModelScope.launch(Dispatchers.Default) {
            val created = createRecogniser()
            synchronized(lock) {
                if (cleared) {
                    created.close()
                } else {
                    recogniser = created
                    live = LiveRecognition(created)
                    mutableState.update { it.copy(ready = true) }
                }
            }
        }
    }

    /** Called on the camera's analysis thread, one frame at a time; blocks until the frame is done. */
    fun analyse(
        frame: RecognitionFrame,
        crop: BoundingBox?,
    ) {
        if (mutableState.value.frozen) return
        val phrases = synchronized(lock) { live?.let { process(it, frame, crop) } } ?: return
        val snapshot = if (mutableState.value.freezeRequested) captureFrame(frame, crop) else null
        mutableState.update { state ->
            if (state.freezeRequested) {
                state.copy(phrases = phrases, freezeRequested = false, frozen = true, frozenImage = snapshot)
            } else {
                state.copy(phrases = phrases)
            }
        }
    }

    fun toggleFreeze() {
        mutableState.update { state ->
            when {
                state.frozen -> state.copy(frozen = false, frozenImage = null)
                else -> state.copy(freezeRequested = !state.freezeRequested)
            }
        }
    }

    fun toggleTorch() {
        mutableState.update { it.copy(torchOn = !it.torchOn) }
    }

    fun setTorchAvailable(available: Boolean) {
        mutableState.update { it.copy(torchAvailable = available, torchOn = it.torchOn && available) }
    }

    private fun process(
        current: LiveRecognition,
        frame: RecognitionFrame,
        crop: BoundingBox?,
    ): List<Phrase>? =
        try {
            runBlocking { current.process(frame, crop) }
        } catch (_: IllegalStateException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    fun togglePhrase(id: Long) {
        mutableState.update { state ->
            val phrase = state.phrases.firstOrNull { it.id == id }
            if (phrase == null) {
                state
            } else {
                state.selection.toggled(phrase).let { state.copy(selection = it, fieldText = it.tappedText) }
            }
        }
    }

    fun selectRange(range: TextSelection) {
        mutableState.update { state ->
            val text = PhraseSelection.text(state.phrases, range)
            if (text == null) state else state.copy(selection = state.selection.dragged(range), fieldText = text)
        }
    }

    fun clearText() {
        mutableState.update { it.copy(selection = OcrSelection(), fieldText = "") }
    }

    fun editText(text: String) {
        mutableState.update { it.copy(fieldText = text) }
    }

    override fun onCleared() {
        synchronized(lock) {
            cleared = true
            live = null
            recogniser?.close()
            recogniser = null
        }
    }
}
