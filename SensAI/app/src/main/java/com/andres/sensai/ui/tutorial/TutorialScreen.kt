package com.andres.sensai.ui.tutorial

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.ui.navigation.NavRoutes
import com.andres.sensai.ui.training.CameraPermissionGate
import com.andres.sensai.ui.training.CameraPoseView
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

@Composable
fun TutorialScreen(
    navController: NavController,
    exerciseId: String
) {
    CameraPermissionGate(
        deniedText = "Necesito permiso de cámara para el tutorial.",
        content = {
            TutorialContent(
                navController = navController,
                exerciseId = exerciseId
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorialContent(
    navController: NavController,
    exerciseId: String
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSquat = exerciseId == "squat"

    val validator: TutorialValidator = remember(exerciseId) {
        if (isSquat) {
            SquatTutorialValidator()
        } else {
            PushUpTutorialValidator()
        }
    }

    val initialFeedback = TutorialFeedback(
        phase = TutorialPhase.READY,
        isCorrect = false,
        title = "Fase 1 · Inicio",
        instruction = "Colócate correctamente.",
        correction = "",
        stableMs = 0L
    )

    var feedback by remember(exerciseId) { mutableStateOf(initialFeedback) }
    var landmarks by remember { mutableStateOf<List<NormalizedLandmark>>(emptyList()) }

    val accent = when {
        feedback.phase == TutorialPhase.DONE -> Color(0xFF4CAF50)
        feedback.isCorrect -> Color(0xFF4CAF50)
        else -> Color(0xFFE53935)
    }

    val trainRoute = if (isSquat) NavRoutes.TRAIN_SQUAT else NavRoutes.TRAIN_PUSHUP
    val totalPhases = 4
    val currentIndex = tutorialPhaseIndex(feedback.phase)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isSquat) "Tutorial · Sentadilla" else "Tutorial · Flexión",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF05070A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF05070A)
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF05070A),
                            Color(0xFF0B1220),
                            Color(0xFF05070A)
                        )
                    )
                )
        ) {
            if (isLandscape) {
                val leftWidth = maxWidth * 0.60f
                val rightWidth = maxWidth * 0.32f

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TutorialCameraPanel(
                        modifier = Modifier
                            .width(leftWidth)
                            .fillMaxHeight(),
                        landmarks = landmarks,
                        accent = accent,
                        feedback = feedback,
                        onLandmarks = { lm, ts ->
                            landmarks = lm
                            feedback = validator.update(lm, ts)
                        }
                    )

                    TutorialInfoPanel(
                        modifier = Modifier
                            .width(rightWidth)
                            .fillMaxHeight(),
                        isSquat = isSquat,
                        feedback = feedback,
                        accent = accent,
                        currentIndex = currentIndex,
                        totalPhases = totalPhases,
                        onReset = {
                            validator.reset()
                            feedback = initialFeedback
                        },
                        onStart = { navController.navigate(trainRoute) }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    TutorialCameraPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        landmarks = landmarks,
                        accent = accent,
                        feedback = feedback,
                        onLandmarks = { lm, ts ->
                            landmarks = lm
                            feedback = validator.update(lm, ts)
                        }
                    )

                    TutorialInfoPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        isSquat = isSquat,
                        feedback = feedback,
                        accent = accent,
                        currentIndex = currentIndex,
                        totalPhases = totalPhases,
                        onReset = {
                            validator.reset()
                            feedback = initialFeedback
                        },
                        onStart = { navController.navigate(trainRoute) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialCameraPanel(
    modifier: Modifier = Modifier,
    landmarks: List<NormalizedLandmark>,
    accent: Color,
    feedback: TutorialFeedback,
    onLandmarks: (List<NormalizedLandmark>, Long) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.22f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPoseView(
                modifier = Modifier.fillMaxSize(),
                drawOverlay = false,
                mirror = true,
                onLandmarks = onLandmarks
            )

            TutorialPoseOverlay(
                landmarks = landmarks,
                color = accent,
                mirror = true,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.18f)
            ) {
                Text(
                    text = when {
                        feedback.phase == TutorialPhase.DONE -> "Listo"
                        feedback.isCorrect -> "Correcto"
                        else -> "Corrige"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = accent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TutorialInfoPanel(
    modifier: Modifier = Modifier,
    isSquat: Boolean,
    feedback: TutorialFeedback,
    accent: Color,
    currentIndex: Int,
    totalPhases: Int,
    onReset: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1624)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TutorialProgressRow(
                currentIndex = currentIndex,
                total = totalPhases
            )

            Text(
                text = feedback.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = feedback.instruction,
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyLarge
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (feedback.phase == TutorialPhase.DONE) {
                        "Tutorial completado. Ya puedes entrenar."
                    } else if (feedback.isCorrect) {
                        "Bien. Mantén la fase para avanzar."
                    } else {
                        "Corrección: ${feedback.correction}"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    color = accent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = {
                    if (feedback.phase == TutorialPhase.DONE) {
                        1f
                    } else {
                        (feedback.stableMs / 700f).coerceIn(0f, 1f)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Text(
                text = if (isSquat) {
                    "Colócate frontal o semi-frontal para este tutorial."
                } else {
                    "Colócate de perfil para este tutorial."
                },
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A2232),
                        contentColor = Color.White
                    )
                ) {
                    Text("Reiniciar")
                }

                Button(
                    onClick = onStart,
                    enabled = feedback.phase == TutorialPhase.DONE,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Empezar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialProgressRow(
    currentIndex: Int,
    total: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val color = when {
                index < currentIndex -> Color(0xFF4CAF50)
                index == currentIndex -> Color(0xFFFFB300)
                else -> Color.White.copy(alpha = 0.18f)
            }

            Surface(
                modifier = Modifier.size(18.dp),
                shape = RoundedCornerShape(50),
                color = color
            ) {}
        }
    }
}

private fun tutorialPhaseIndex(phase: TutorialPhase): Int {
    return when (phase) {
        TutorialPhase.READY -> 0
        TutorialPhase.DOWN -> 1
        TutorialPhase.BOTTOM -> 2
        TutorialPhase.UP -> 3
        TutorialPhase.DONE -> 4
    }
}