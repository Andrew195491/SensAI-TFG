package com.andres.sensai.ui.training

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.core.analysis.PoseAnalyzer
import com.andres.sensai.core.camera.CameraPreview
import com.andres.sensai.core.camera.rememberCameraPermissionState
import com.andres.sensai.core.pose.PoseResult
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingCameraScreen(
    navController: NavController,
    exercise: String,
    onFinish: () -> Unit
) {
    val hasPermission = rememberCameraPermissionState().value

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    val context = LocalContext.current
    var pose by remember { mutableStateOf<PoseResult?>(null) }

    val analyzer = remember {
        PoseAnalyzer(
            context = context,
            isFrontCamera = true,
            onPose = { result -> pose = result }
        )
    }
    DisposableEffect(Unit) { onDispose { analyzer.close() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenar: ${exercise.replaceFirstChar { it.uppercaseChar() }}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (hasPermission) {
                // Preview + análisis
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    analyzer = analyzer,
                    analysisExecutor = analysisExecutor,
                    lensFacing = CameraSelector.LENS_FACING_BACK
                )

                // Overlay de puntos (si hay pose)
                PoseOverlay(
                    pose = pose,
                    modifier = Modifier.fillMaxSize(),
                    minScore = 0f     // para depurar
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Permiso de cámara requerido")
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ejercicio: ${exercise.replaceFirstChar { it.uppercaseChar() }}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Inferencia: ${pose?.inferenceMs?.let { "$it ms" } ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Terminar sesión (demo)")
                }
            }
        }
    }
}
