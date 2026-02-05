package com.andres.sensai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andres.sensai.core.camera.CameraPreview

@Composable
fun WorkoutScreen(
    onFinishWorkout: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Cámara ocupando toda la pantalla
        CameraPreview(
            modifier = Modifier.fillMaxSize()
        )

        // Controles por encima
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Pantalla de Entrenamiento")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onFinishWorkout) {
                Text("Terminar sesión")
            }
        }
    }
}
