package com.ravendarque.deaftly.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.LiveRecognition
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import com.ravendarque.deaftly.core.ocr.TextRecogniser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class OcrUiState(
    val ready: Boolean = false,
    val phrases: List<Phrase> = emptyList(),
)

class OcrViewModel(
    private val createRecogniser: () -> TextRecogniser,
) : ViewModel() {
    private val lock = Any()
    private var recogniser: TextRecogniser? = null
    private var live: LiveRecognition? = null
    private var cleared = false
    private val mutableState = MutableStateFlow(OcrUiState())

    val state: StateFlow<OcrUiState> = mutableState

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val created = createRecogniser()
            synchronized(lock) {
                if (cleared) {
                    created.close()
                } else {
                    recogniser = created
                    live = LiveRecognition(created)
                    mutableState.value = mutableState.value.copy(ready = true)
                }
            }
        }
    }

    /** Called on the camera's analysis thread, one frame at a time; blocks until the frame is done. */
    fun analyse(
        frame: RecognitionFrame,
        crop: BoundingBox?,
    ) {
        val phrases = synchronized(lock) { live?.let { process(it, frame, crop) } }
        if (phrases != null) mutableState.value = mutableState.value.copy(phrases = phrases)
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

    override fun onCleared() {
        synchronized(lock) {
            cleared = true
            live = null
            recogniser?.close()
            recogniser = null
        }
    }
}
