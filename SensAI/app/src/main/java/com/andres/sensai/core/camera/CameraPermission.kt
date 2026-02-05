package com.andres.sensai.core.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberCameraPermissionState(): State<Boolean> {
    val context = LocalContext.current
    val granted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted.value = isGranted
    }

    LaunchedEffect(Unit) {
        if (!granted.value) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    return granted
}
