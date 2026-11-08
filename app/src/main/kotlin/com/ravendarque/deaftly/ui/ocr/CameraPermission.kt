package com.ravendarque.deaftly.ui.ocr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class CameraAccess {
    NotAsked,
    Denied,
    DeniedForGood,
    Granted,
}

class CameraPermission(
    val access: CameraAccess,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
fun rememberCameraPermission(): CameraPermission {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            granted = result
            asked = true
            blocked = !result && !context.canAskForCamera()
        }
    LifecycleResumeEffect(Unit) {
        granted = context.hasCameraPermission()
        onPauseOrDispose {}
    }
    val access =
        when {
            granted -> CameraAccess.Granted
            !asked -> CameraAccess.NotAsked
            blocked -> CameraAccess.DeniedForGood
            else -> CameraAccess.Denied
        }
    return CameraPermission(
        access = access,
        request = { launcher.launch(Manifest.permission.CAMERA) },
        openSettings = { context.openAppSettings() },
    )
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.canAskForCamera(): Boolean {
    val activity = findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
}

private fun Context.openAppSettings() {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
