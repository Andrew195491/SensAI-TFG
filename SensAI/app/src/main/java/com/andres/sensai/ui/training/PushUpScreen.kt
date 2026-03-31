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
fun PushUpScreen(navController: NavController) {
    CameraPermissionGate(
        deniedText = "Necesito permiso de cámara para entrenar.",
        content = {
            val detector = remember { PushUpDetector() }

            var reps by remember { mutableIntStateOf(0) }
            var state by remember { mutableStateOf(PushUpDetector.State.WAIT_UP) }
            var score by remember { mutableFloatStateOf(1f) }

            var elbowL by remember { mutableFloatStateOf(180f) }
            var elbowR by remember { mutableFloatStateOf(180f) }
            var stableMs by remember { mutableLongStateOf(0L) }

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Training: Flexiones") },
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
                    CameraPoseView(
                        modifier = Modifier.fillMaxSize(),
                        drawOverlay = true,
                        mirror = true,
                        onLandmarks = { landmarks, ts ->
                            val out = detector.update(landmarks, ts)
                            reps = out.reps
                            state = out.state
                            score = out.debug.smoothScore
                            elbowL = out.debug.elbowAngleL
                            elbowR = out.debug.elbowAngleR
                            stableMs = out.debug.stableMs
                        }
                    )

                    Text(
                        text = buildString {
                            append("REPS: $reps\n")
                            append("STATE: ${state.name}\n")
                            append("SCORE: ${String.format(Locale.US, "%.2f", score)}\n")
                            append("stable=${stableMs}ms\n")
                            append("elbowL=${elbowL.toInt()} elbowR=${elbowR.toInt()}")
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
    )
}