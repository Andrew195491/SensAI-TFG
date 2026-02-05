package com.andres.sensai.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.core.camera.CameraPreview
import com.andres.sensai.core.camera.rememberCameraPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialCameraScreen(
    navController: NavController,
    exercise: String
) {
    val hasPermission = rememberCameraPermissionState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorial: ${exercise.replaceFirstChar { it.uppercaseChar() }}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Parte superior: placeholder tutorial
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aquí irá el tutorial visual del ejercicio")
            }

            // Parte inferior: cámara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (hasPermission) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Permiso de cámara requerido")
                    }
                }
            }
        }
    }
}
