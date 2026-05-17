package com.andres.sensai.ui.training

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.andres.sensai.ui.profile.ProfileManager
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TrainScreen(
    navController: NavController,
    exerciseType: ExerciseType
) {
    CameraPermissionGate(
        deniedText = "Necesito permiso de cámara para entrenar.",
        content = {
            TrainContent(
                navController = navController,
                exerciseType = exerciseType
            )
        }
    )
}

@Composable
private fun TrainContent(
    navController: NavController,
    exerciseType: ExerciseType
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showDevTools = remember { isDebuggableApp(context) }

    val squatDetector = remember {
        if (exerciseType == ExerciseType.SQUAT) SquatDetector() else null
    }
    val pushUpDetector = remember {
        if (exerciseType == ExerciseType.PUSHUP) PushUpDetector() else null
    }

    var reps by remember { mutableIntStateOf(0) }
    var stateText by remember { mutableStateOf("WAIT_UP") }
    var score by remember { mutableFloatStateOf(1f) }

    var rawScore by remember { mutableFloatStateOf(1f) }
    var stableMs by remember { mutableLongStateOf(0L) }
    var angleA by remember { mutableFloatStateOf(180f) }
    var angleB by remember { mutableFloatStateOf(180f) }

    var showDebug by remember { mutableStateOf(false) }
    var showRewardBanner by remember { mutableStateOf(false) }
    var rewardBannerText by remember { mutableStateOf("") }

    var challenge by remember(exerciseType) {
        mutableStateOf(DailyChallengeManager.getTodayChallenge(context, exerciseType))
    }

    var challengeProgress by remember(exerciseType) {
        mutableIntStateOf(DailyChallengeManager.getProgress(context, challenge))
    }

    var challengeCompleted by remember(exerciseType) {
        mutableStateOf(DailyChallengeManager.isCompleted(context, challenge))
    }

    var lastSessionReps by remember { mutableIntStateOf(0) }

    fun resetSessionState() {
        when (exerciseType) {
            ExerciseType.SQUAT -> squatDetector?.reset()
            ExerciseType.PUSHUP -> pushUpDetector?.reset()
        }

        reps = 0
        lastSessionReps = 0
        stateText = "WAIT_UP"
        score = 1f
        rawScore = 1f
        stableMs = 0L
        angleA = 180f
        angleB = 180f
    }

    fun reloadChallengeAndResetSession() {
        challenge = DailyChallengeManager.getTodayChallenge(context, exerciseType)
        challengeProgress = DailyChallengeManager.getProgress(context, challenge)
        challengeCompleted = DailyChallengeManager.isCompleted(context, challenge)
        resetSessionState()
    }

    LaunchedEffect(reps) {
        if (reps > lastSessionReps) {
            val delta = reps - lastSessionReps
            challengeProgress = DailyChallengeManager.addProgress(context, challenge, delta)

            if (!challengeCompleted && challengeProgress >= challenge.targetReps) {
                val xpReward = ProfileManager.getDailyChallengeXpReward(exerciseType)
                val completedNow = DailyChallengeManager.completeChallenge(context, challenge)

                challengeCompleted = true

                if (completedNow) {
                    rewardBannerText = "¡Reto diario completado! +$xpReward XP"
                    showRewardBanner = true
                }
            }
        }

        lastSessionReps = reps
    }

    LaunchedEffect(showRewardBanner) {
        if (showRewardBanner) {
            delay(2500)
            showRewardBanner = false
        }
    }

    val stateColor = exerciseStateColor(stateText)
    val depthProgress = (1f - score).coerceIn(0f, 1f)
    val safeChallengeProgress = challengeProgress.coerceAtMost(challenge.targetReps)

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPoseView(
            modifier = Modifier.fillMaxSize(),
            drawOverlay = true,
            mirror = true,
            onLandmarks = { landmarks, ts ->
                when (exerciseType) {
                    ExerciseType.SQUAT -> {
                        val out = squatDetector?.update(landmarks, ts) ?: return@CameraPoseView
                        reps = out.reps
                        stateText = out.state.name
                        score = out.debug.smoothScore
                        rawScore = out.debug.rawScore
                        stableMs = out.debug.stableMs
                        angleA = out.debug.leftKneeAngle
                        angleB = out.debug.rightKneeAngle
                    }

                    ExerciseType.PUSHUP -> {
                        val out = pushUpDetector?.update(landmarks, ts) ?: return@CameraPoseView
                        reps = out.reps
                        stateText = out.state.name
                        score = out.debug.smoothScore
                        rawScore = out.debug.rawScore
                        stableMs = out.debug.stableMs
                        angleA = out.debug.elbowAngleL
                        angleB = out.debug.elbowAngleR
                    }
                }
            }
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.45f)
                        .fillMaxHeight()
                ) {
                    TopOverlayRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        navController = navController,
                        showDevTools = showDevTools,
                        onAdvanceDay = {
                            DailyChallengeManager.advanceDeveloperDay(context)
                            reloadChallengeAndResetSession()
                            rewardBannerText = "Día +1 aplicado. Reto reiniciado."
                            showRewardBanner = true
                        },
                        onResetDay = {
                            DailyChallengeManager.resetDeveloperDay(context)
                            reloadChallengeAndResetSession()
                            rewardBannerText = "Fecha real restaurada."
                            showRewardBanner = true
                        },
                        showDebug = showDebug,
                        onToggleDebug = { showDebug = !showDebug }
                    )

                    if (showRewardBanner) {
                        RewardBanner(
                            text = rewardBannerText,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 64.dp)
                        )
                    }

                    RepsChallengeCard(
                        exerciseType = exerciseType,
                        reps = reps,
                        challengeCompleted = challengeCompleted,
                        safeChallengeProgress = safeChallengeProgress,
                        challengeTarget = challenge.targetReps,
                        showDevTools = showDevTools,
                        currentDayLabel = DailyChallengeManager.getCurrentDayLabel(context),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 64.dp)
                    )

                    StatusPill(
                        text = stateText,
                        color = stateColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 72.dp)
                    )

                    if (showDebug) {
                        DebugCard(
                            exerciseType = exerciseType,
                            score = score,
                            rawScore = rawScore,
                            stableMs = stableMs,
                            angleA = angleA,
                            angleB = angleB,
                            showDevTools = showDevTools,
                            dayOffset = DailyChallengeManager.getDeveloperDayOffset(context),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.42f)
                        )
                    }
                }

                SideInfoPanel(
                    exerciseType = exerciseType,
                    stateText = stateText,
                    stateColor = stateColor,
                    depthProgress = depthProgress,
                    onResetSession = { resetSessionState() },
                    modifier = Modifier
                        .weight(0.72f)
                        .fillMaxHeight()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                TopOverlayRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    navController = navController,
                    showDevTools = showDevTools,
                    onAdvanceDay = {
                        DailyChallengeManager.advanceDeveloperDay(context)
                        reloadChallengeAndResetSession()
                        rewardBannerText = "Día +1 aplicado. Reto reiniciado."
                        showRewardBanner = true
                    },
                    onResetDay = {
                        DailyChallengeManager.resetDeveloperDay(context)
                        reloadChallengeAndResetSession()
                        rewardBannerText = "Fecha real restaurada."
                        showRewardBanner = true
                    },
                    showDebug = showDebug,
                    onToggleDebug = { showDebug = !showDebug }
                )

                if (showRewardBanner) {
                    RewardBanner(
                        text = rewardBannerText,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 64.dp)
                    )
                }

                RepsChallengeCard(
                    exerciseType = exerciseType,
                    reps = reps,
                    challengeCompleted = challengeCompleted,
                    safeChallengeProgress = safeChallengeProgress,
                    challengeTarget = challenge.targetReps,
                    showDevTools = showDevTools,
                    currentDayLabel = DailyChallengeManager.getCurrentDayLabel(context),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 64.dp)
                )

                StatusPill(
                    text = stateText,
                    color = stateColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 72.dp)
                )

                if (showDebug) {
                    DebugCard(
                        exerciseType = exerciseType,
                        score = score,
                        rawScore = rawScore,
                        stableMs = stableMs,
                        angleA = angleA,
                        angleB = angleB,
                        showDevTools = showDevTools,
                        dayOffset = DailyChallengeManager.getDeveloperDayOffset(context),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.62f)
                    )
                }

                BottomInfoPanel(
                    exerciseType = exerciseType,
                    stateText = stateText,
                    stateColor = stateColor,
                    depthProgress = depthProgress,
                    onResetSession = { resetSessionState() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun TopOverlayRow(
    modifier: Modifier = Modifier,
    navController: NavController,
    showDevTools: Boolean,
    onAdvanceDay: () -> Unit,
    onResetDay: () -> Unit,
    showDebug: Boolean,
    onToggleDebug: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleOverlayButton(
            onClick = { navController.popBackStack() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDevTools) {
                TextButton(
                    onClick = onAdvanceDay,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF1565C0).copy(alpha = 0.75f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Text(
                        text = "Día +1",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onResetDay,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF455A64).copy(alpha = 0.75f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Text(
                        text = "Hoy",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = onToggleDebug,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Text(
                    text = if (showDebug) "Ocultar debug" else "Debug",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RepsChallengeCard(
    exerciseType: ExerciseType,
    reps: Int,
    challengeCompleted: Boolean,
    safeChallengeProgress: Int,
    challengeTarget: Int,
    showDevTools: Boolean,
    currentDayLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (exerciseType == ExerciseType.SQUAT) "Sentadilla" else "Flexión",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )

            Text(
                text = reps.toString(),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "repeticiones",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (challengeCompleted) {
                    Color(0xFF4CAF50).copy(alpha = 0.18f)
                } else {
                    Color(0xFFFFB300).copy(alpha = 0.18f)
                }
            ) {
                Text(
                    text = "$safeChallengeProgress/$challengeTarget",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (challengeCompleted) Color(0xFF4CAF50) else Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (challengeCompleted) {
                    "Reto diario completado"
                } else {
                    "Reto diario"
                },
                color = if (challengeCompleted) {
                    Color(0xFF4CAF50)
                } else {
                    Color.White.copy(alpha = 0.72f)
                },
                fontSize = 11.sp,
                fontWeight = if (challengeCompleted) FontWeight.SemiBold else FontWeight.Normal
            )

            if (showDevTools) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "día: $currentDayLabel",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DebugCard(
    exerciseType: ExerciseType,
    score: Float,
    rawScore: Float,
    stableMs: Long,
    angleA: Float,
    angleB: Float,
    showDevTools: Boolean,
    dayOffset: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Debug",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            DebugText("score: ${String.format(Locale.US, "%.2f", score)}")
            DebugText("raw: ${String.format(Locale.US, "%.2f", rawScore)}")
            DebugText("stable: ${stableMs} ms")
            DebugText(
                if (exerciseType == ExerciseType.SQUAT) {
                    "kneeL: ${angleA.toInt()}°"
                } else {
                    "elbowL: ${angleA.toInt()}°"
                }
            )
            DebugText(
                if (exerciseType == ExerciseType.SQUAT) {
                    "kneeR: ${angleB.toInt()}°"
                } else {
                    "elbowR: ${angleB.toInt()}°"
                }
            )

            if (showDevTools) {
                DebugText("dayOffset: $dayOffset")
            }
        }
    }
}

@Composable
private fun BottomInfoPanel(
    exerciseType: ExerciseType,
    stateText: String,
    stateColor: Color,
    depthProgress: Float,
    onResetSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (exerciseType == ExerciseType.SQUAT) "Profundidad" else "Progreso",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${(depthProgress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            LinearProgressIndicator(
                progress = { depthProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = stateColor,
                trackColor = Color.White.copy(alpha = 0.18f)
            )

            Text(
                text = exerciseTip(exerciseType, stateText),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onResetSession,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = stateColor
                    )
                ) {
                    Text("Reiniciar")
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = when (exerciseType) {
                            ExerciseType.SQUAT -> "Frontal o semi-frontal"
                            ExerciseType.PUSHUP -> "Vista lateral"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SideInfoPanel(
    exerciseType: ExerciseType,
    stateText: String,
    stateColor: Color,
    depthProgress: Float,
    onResetSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (exerciseType == ExerciseType.SQUAT) "Profundidad" else "Progreso",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            LinearProgressIndicator(
                progress = { depthProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = stateColor,
                trackColor = Color.White.copy(alpha = 0.18f)
            )

            Text(
                text = "${(depthProgress * 100).toInt()}%",
                color = Color.White,
                fontSize = 13.sp
            )

            Text(
                text = exerciseTip(exerciseType, stateText),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onResetSession,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = stateColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reiniciar")
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.10f)
            ) {
                Text(
                    text = when (exerciseType) {
                        ExerciseType.SQUAT -> "Frontal o semi-frontal"
                        ExerciseType.PUSHUP -> "Vista lateral"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CircleOverlayButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f)
    ) {
        IconButton(onClick = onClick) {
            content()
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.22f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DebugText(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 12.sp
    )
}

@Composable
private fun RewardBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.95f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

private fun exerciseStateColor(stateText: String): Color {
    return when (stateText) {
        "WAIT_UP" -> Color(0xFF4CAF50)
        "GOING_DOWN" -> Color(0xFFFFB300)
        "WAIT_DOWN" -> Color(0xFF29B6F6)
        "GOING_UP" -> Color(0xFFFF7043)
        else -> Color(0xFF9E9E9E)
    }
}

private fun exerciseTip(
    exerciseType: ExerciseType,
    stateText: String
): String {
    return when (exerciseType) {
        ExerciseType.SQUAT -> {
            when (stateText) {
                "WAIT_UP" -> "Empieza estable y baja con control."
                "GOING_DOWN" -> "Sigue bajando sin perder la postura."
                "WAIT_DOWN" -> "Has llegado abajo. Mantén un instante la posición."
                "GOING_UP" -> "Sube completo hasta volver arriba."
                else -> "Colócate bien en cámara y empieza el movimiento."
            }
        }

        ExerciseType.PUSHUP -> {
            when (stateText) {
                "WAIT_UP" -> "Empieza arriba con el cuerpo alineado."
                "GOING_DOWN" -> "Flexiona los codos y baja con control."
                "WAIT_DOWN" -> "Has llegado abajo. Mantén un instante la posición."
                "GOING_UP" -> "Empuja hasta volver arriba del todo."
                else -> "Colócate de perfil y prepara la postura."
            }
        }
    }
}

private fun isDebuggableApp(context: Context): Boolean {
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}