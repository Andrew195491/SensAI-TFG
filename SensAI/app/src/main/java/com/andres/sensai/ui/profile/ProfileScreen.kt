package com.andres.sensai.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.ui.training.DailyChallengeManager
import com.andres.sensai.ui.training.ExerciseType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val profile = ProfileManager.getProfileProgress(context)

    val squatChallenge = DailyChallengeManager.getTodayChallenge(context, ExerciseType.SQUAT)
    val pushupChallenge = DailyChallengeManager.getTodayChallenge(context, ExerciseType.PUSHUP)

    val squatProgress = DailyChallengeManager.getProgress(context, squatChallenge)
    val pushupProgress = DailyChallengeManager.getProgress(context, pushupChallenge)

    val squatCompleted = DailyChallengeManager.isCompleted(context, squatChallenge)
    val pushupCompleted = DailyChallengeManager.isCompleted(context, pushupChallenge)

    val levelProgress =
        if (profile.xpForNextLevel > 0) {
            profile.xpIntoCurrentLevel.toFloat() / profile.xpForNextLevel.toFloat()
        } else {
            0f
        }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Perfil",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF05070A),
                            Color(0xFF0B1220),
                            Color(0xFF05070A)
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.28f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(110.dp),
                            shape = CircleShape,
                            color = Color(0xFF121A28)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "S",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp),
                            shape = CircleShape,
                            color = Color(0xFFFFB300)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile.level.toString(),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Nivel ${profile.level}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${profile.totalXp} XP totales",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    LinearProgressIndicator(
                        progress = { levelProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF29B6F6),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    Text(
                        text = "${profile.xpIntoCurrentLevel}/${profile.xpForNextLevel} XP para el siguiente nivel",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Bolt,
                    value = "${ProfileManager.getDailyChallengeXpReward(ExerciseType.SQUAT)} XP",
                    label = "Por reto"
                )

                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.EmojiEvents,
                    value = profile.completedDailyChallenges.toString(),
                    label = "Retos completados"
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1624)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Retos de hoy",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TodayChallengeRow(
                        title = "Sentadilla",
                        progress = squatProgress,
                        target = squatChallenge.targetReps,
                        completed = squatCompleted
                    )

                    TodayChallengeRow(
                        title = "Flexión",
                        progress = pushupProgress,
                        target = pushupChallenge.targetReps,
                        completed = pushupCompleted
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.26f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = Color(0xFFFFB300).copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFFFB300)
                    )
                }
            }

            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = label,
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TodayChallengeRow(
    title: String,
    progress: Int,
    target: Int,
    completed: Boolean
) {
    val safeProgress = progress.coerceAtMost(target)
    val fraction = if (target > 0) safeProgress.toFloat() / target.toFloat() else 0f
    val accent = if (completed) Color(0xFF4CAF50) else Color(0xFFFFB300)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "$safeProgress/$target",
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (completed) Icons.Filled.Star else Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = accent
                )

                Text(
                    text = if (completed) "Completado" else "Pendiente",
                    color = if (completed) accent else Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}