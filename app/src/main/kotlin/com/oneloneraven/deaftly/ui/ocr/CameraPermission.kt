package com.oneloneraven.deaftly.ui.ocr

import android.Manifest
import androidx.compose.runtime.Composable
import com.oneloneraven.deaftly.ui.permissions.PermissionAccess
import com.oneloneraven.deaftly.ui.permissions.RuntimePermission
import com.oneloneraven.deaftly.ui.permissions.rememberRuntimePermission

typealias CameraAccess = PermissionAccess

typealias CameraPermission = RuntimePermission

@Composable
fun rememberCameraPermission(): CameraPermission = rememberRuntimePermission(Manifest.permission.CAMERA)
