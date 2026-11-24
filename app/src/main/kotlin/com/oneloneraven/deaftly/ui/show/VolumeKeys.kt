package com.oneloneraven.deaftly.ui.show

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

class VolumeKeys {
    private var onStep: ((larger: Boolean) -> Unit)? = null

    fun claim(handler: (larger: Boolean) -> Unit) {
        onStep = handler
    }

    fun release(handler: (larger: Boolean) -> Unit) {
        if (onStep === handler) onStep = null
    }

    fun handle(event: KeyEvent): Boolean {
        val step = onStep
        val larger = largerFor(event.keyCode)
        if (step == null || larger == null) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) step(larger)
        return true
    }

    private fun largerFor(keyCode: Int): Boolean? =
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> true
            KeyEvent.KEYCODE_VOLUME_DOWN -> false
            else -> null
        }
}

val LocalVolumeKeys = staticCompositionLocalOf<VolumeKeys?> { null }

@Composable
fun VolumeKeysEffect(onStep: (larger: Boolean) -> Unit) {
    val keys = LocalVolumeKeys.current ?: return
    val current by rememberUpdatedState(onStep)
    DisposableEffect(keys) {
        val handler: (Boolean) -> Unit = { current(it) }
        keys.claim(handler)
        onDispose { keys.release(handler) }
    }
}
