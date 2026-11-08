package com.ravendarque.deaftly.ui.ocr

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.awaitCancellation

@Composable
fun CameraViewfinder(
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    val preview =
        remember {
            Preview.Builder().build().apply { setSurfaceProvider { surfaceRequest = it } }
        }
    LaunchedEffect(lifecycleOwner, preview) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        if (!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            onUnavailable()
            return@LaunchedEffect
        }
        try {
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            awaitCancellation()
        } catch (_: IllegalArgumentException) {
            onUnavailable()
        } finally {
            provider.unbind(preview)
        }
    }
    surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = modifier) }
}
