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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.ocr.Phrase
import com.ravendarque.deaftly.ui.theme.CameraColours

@Composable
fun OcrScreen(
    viewModelFactory: ViewModelProvider.Factory,
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
        val viewModel = viewModel<OcrViewModel>(factory = viewModelFactory)
        val state by viewModel.state.collectAsStateWithLifecycle()
        OcrCameraContent(
            status = stringResource(if (state.ready) R.string.ocr_status_live else R.string.ocr_status_starting),
            phrases = state.phrases,
            onClose = onClose,
            controlsEnabled = false,
            viewfinder = { CameraViewfinder(viewModel::analyse, onUnavailable = { unavailable = true }, modifier = it) },
        )
    }
}

@Composable
fun OcrCameraContent(
    status: String,
    phrases: List<Phrase>,
    onClose: () -> Unit,
    controlsEnabled: Boolean,
    viewfinder: @Composable (Modifier) -> Unit,
) {
    LightSystemBarIcons()
    val cameraView = stringResource(R.string.ocr_view_description)
    Box(modifier = Modifier.fillMaxSize().background(CameraColours.backdrop)) {
        Box(modifier = Modifier.fillMaxSize().semantics { contentDescription = cameraView }) {
            viewfinder(Modifier.fillMaxSize())
            PhraseOverlay(phrases, Modifier.fillMaxSize())
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
