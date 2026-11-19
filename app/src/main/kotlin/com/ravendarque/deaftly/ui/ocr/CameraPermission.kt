package com.ravendarque.deaftly.ui.ocr

import android.Manifest
import androidx.compose.runtime.Composable
import com.ravendarque.deaftly.ui.permissions.PermissionAccess
import com.ravendarque.deaftly.ui.permissions.RuntimePermission
import com.ravendarque.deaftly.ui.permissions.rememberRuntimePermission

typealias CameraAccess = PermissionAccess

typealias CameraPermission = RuntimePermission

@Composable
fun rememberCameraPermission(): CameraPermission = rememberRuntimePermission(Manifest.permission.CAMERA)
