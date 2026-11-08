package com.ravendarque.deaftly.ui.ocr

import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ravendarque.deaftly.core.ocr.BoundingBox
import com.ravendarque.deaftly.core.ocr.RecognitionFrame
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.Executors

private const val ANALYSIS_LONG_SIDE = 960
private const val ANALYSIS_SHORT_SIDE = 720

@Composable
fun CameraViewfinder(
    analyse: (RecognitionFrame, BoundingBox?) -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val rotation = LocalView.current.display?.rotation ?: Surface.ROTATION_0
    val currentAnalyse by rememberUpdatedState(analyse)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) { onDispose { executor.shutdown() } }
    val preview = remember { Preview.Builder().build().apply { setSurfaceProvider { surfaceRequest = it } } }
    val imageAnalysis =
        remember {
            ImageAnalysis
                .Builder()
                .setResolutionSelector(analysisResolution())
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply { setAnalyzer(executor, FrameAnalyser { frame, crop -> currentAnalyse(frame, crop) }) }
        }
    LaunchedEffect(lifecycleOwner, size, rotation) {
        if (size == IntSize.Zero) return@LaunchedEffect
        val provider = ProcessCameraProvider.awaitInstance(context)
        if (!provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            onUnavailable()
            return@LaunchedEffect
        }
        preview.targetRotation = rotation
        imageAnalysis.targetRotation = rotation
        val viewPort = ViewPort.Builder(Rational(size.width, size.height), rotation).setScaleType(ViewPort.FILL_CENTER).build()
        val group =
            UseCaseGroup
                .Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .build()
        try {
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group)
            awaitCancellation()
        } catch (_: IllegalArgumentException) {
            onUnavailable()
        } finally {
            provider.unbind(preview, imageAnalysis)
        }
    }
    val sized = modifier.onSizeChanged { size = it }
    val request = surfaceRequest
    if (request != null) CameraXViewfinder(surfaceRequest = request, modifier = sized) else Box(sized)
}

private fun analysisResolution(): ResolutionSelector =
    ResolutionSelector
        .Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(Size(ANALYSIS_LONG_SIDE, ANALYSIS_SHORT_SIDE), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
        ).build()
