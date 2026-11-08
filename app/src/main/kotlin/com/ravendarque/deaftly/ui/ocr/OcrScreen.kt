package com.ravendarque.deaftly.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.ui.theme.CameraColours

private val CameraBackdrop = CameraColours.backdrop

@Composable
fun OcrScreen(
    onClose: () -> Unit,
    onTypeInstead: () -> Unit,
) {
    val permission = rememberCameraPermission()
    var unavailable by rememberSaveable { mutableStateOf(false) }
    CameraGate(
        access = permission.access,
        unavailable = unavailable,
        actions = CameraGateActions(permission.request, permission.openSettings, onTypeInstead, onClose),
    ) {
        OcrCameraContent(
            status = stringResource(R.string.ocr_status_live),
            onClose = onClose,
            controlsEnabled = false,
            viewfinder = { CameraViewfinder(onUnavailable = { unavailable = true }, modifier = it) },
        )
    }
}

@Composable
fun OcrCameraContent(
    status: String,
    onClose: () -> Unit,
    controlsEnabled: Boolean,
    viewfinder: @Composable (Modifier) -> Unit,
) {
    LightSystemBarIcons()
    val cameraView = stringResource(R.string.ocr_view_description)
    Box(modifier = Modifier.fillMaxSize().background(CameraBackdrop)) {
        Box(modifier = Modifier.fillMaxSize().semantics { contentDescription = cameraView }) {
            viewfinder(Modifier.fillMaxSize())
        }
        OcrTopBar(
            status = status,
            onClose = onClose,
            controlsEnabled = controlsEnabled,
            modifier = Modifier.statusBarsPadding(),
        )
    }
}

@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val statusWasLight = controller?.isAppearanceLightStatusBars
        val navigationWasLight = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            statusWasLight?.let { controller.isAppearanceLightStatusBars = it }
            navigationWasLight?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }
}
