package com.oneloneraven.deaftly.ui.permissions

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

enum class PermissionAccess {
    NotAsked,
    Denied,
    DeniedForGood,
    Granted,
}

class RuntimePermission(
    val access: PermissionAccess,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
fun rememberRuntimePermission(permission: String): RuntimePermission {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasPermission(permission)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            granted = result
            asked = true
            blocked = !result && !context.canAskFor(permission)
        }
    LifecycleResumeEffect(permission) {
        granted = context.hasPermission(permission)
        onPauseOrDispose {}
    }
    val access =
        when {
            granted -> PermissionAccess.Granted
            !asked -> PermissionAccess.NotAsked
            blocked -> PermissionAccess.DeniedForGood
            else -> PermissionAccess.Denied
        }
    return RuntimePermission(
        access = access,
        request = { launcher.launch(permission) },
        openSettings = { context.openAppSettings() },
    )
}

@Composable
fun rememberMicrophonePermission(): RuntimePermission = rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.canAskFor(permission: String): Boolean {
    val activity = findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
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
