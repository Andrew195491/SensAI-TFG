package com.andres.sensai.ui.home

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.ui.navigation.NavRoutes
import com.andres.sensai.ui.onboarding.UserSetupManager
import com.andres.sensai.ui.profile.ProfileManager
import com.andres.sensai.ui.training.DailyChallengeManager
import com.andres.sensai.ui.training.ExerciseType

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val showDevTools = remember { isDebuggableApp(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05070A),
                        Color(0xFF0B1220),
                        Color(0xFF05070A)
                    )
                )
            )
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.06f)
        ) {
            Text(
                text = "SENSAI",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Entrena con apoyo visual",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        SimpleExerciseCard(
            title = "Sentadilla",
            subtitle = "Control y repeticiones",
            chipText = "Frontal / semi-frontal",
            accent = Color(0xFF29B6F6),
            onTrainClick = { navController.navigate(NavRoutes.TRAIN_SQUAT) },
            onTutorialClick = { navController.navigate(NavRoutes.tutorial("squat")) }
        )

        SimpleExerciseCard(
            title = "Flexión",
            subtitle = "Fases y técnica",
            chipText = "Lateral",
            accent = Color(0xFFFFB300),
            onTrainClick = { navController.navigate(NavRoutes.TRAIN_PUSHUP) },
            onTutorialClick = { navController.navigate(NavRoutes.tutorial("pushup")) }
        )

        Button(
            onClick = { navController.navigate(NavRoutes.PROFILE) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F1A2D),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Perfil",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (showDevTools) {
            DeveloperToolsCard(
                onResetUser = {
                    ProfileManager.resetProfile(context)
                    UserSetupManager.reset(context)
                    DailyChallengeManager.resetAll(context)

                    navController.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SimpleExerciseCard(
    title: String,
    subtitle: String,
    chipText: String,
    accent: Color,
    onTrainClick: () -> Unit,
    onTutorialClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .width(14.dp)
                        .height(88.dp),
                    shape = RoundedCornerShape(50),
                    color = accent
                ) {}

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.74f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = chipText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = accent,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTrainClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Entrenar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onTutorialClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "Tutorial",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperToolsCard(
    onResetUser: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF7043).copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Herramientas de desarrollador",
                color = Color(0xFFFF7043),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Reinicia todo y vuelve a mostrar el onboarding como usuario nuevo.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onResetUser,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7043),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Reiniciar usuario",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun isDebuggableApp(context: Context): Boolean {
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}