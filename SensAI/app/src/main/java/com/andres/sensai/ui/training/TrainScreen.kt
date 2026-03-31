package com.andres.sensai.ui.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainScreen(navController: NavController) {
    CameraPermissionGate(
        deniedText = "Necesito permiso de cámara para entrenar.",
        content = {
            TrainContent(navController)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainContent(navController: NavController) {

    val squatDetector = remember { SquatDetector() }

    var reps by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf(SquatDetector.State.WAIT_UP) }
    var score by remember { mutableFloatStateOf(1f) }

    // Debug opcional
    var rawScore by remember { mutableFloatStateOf(1f) }
    var stableMs by remember { mutableLongStateOf(0L) }
    var kneeL by remember { mutableFloatStateOf(180f) }
    var kneeR by remember { mutableFloatStateOf(180f) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Training (Squat)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ✅ Cámara genérica + pose
            CameraPoseView(
                modifier = Modifier.fillMaxSize(),
                drawOverlay = true,
                mirror = true,
                onLandmarks = { landmarks, ts ->
                    val out = squatDetector.update(landmarks, ts)
                    reps = out.reps
                    state = out.state
                    score = out.debug.smoothScore

                    // Debug opcional
                    rawScore = out.debug.rawScore
                    stableMs = out.debug.stableMs
                    kneeL = out.debug.leftKneeAngle
                    kneeR = out.debug.rightKneeAngle
                }
            )

            // ✅ UI SOLO DEL EJERCICIO (sentadilla)
            Text(
                text = buildString {
                    append("REPS: $reps\n")
                    append("STATE: ${state.name}\n")
                    append("SCORE: ${String.format(Locale.US, "%.2f", score)}\n")
                    append("raw=${String.format(Locale.US, "%.2f", rawScore)} stable=${stableMs}ms\n")
                    append("kneeL=${kneeL.toInt()} kneeR=${kneeR.toInt()}")
                },
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
        }
    }
}