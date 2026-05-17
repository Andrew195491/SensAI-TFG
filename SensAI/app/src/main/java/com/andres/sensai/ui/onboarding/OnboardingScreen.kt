package com.andres.sensai.ui.onboarding

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.ui.navigation.NavRoutes
import java.util.Calendar

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedSex by remember { mutableStateOf<UserSex?>(null) }
    var selectedGoal by remember { mutableStateOf<TrainingGoal?>(null) }

    var birthYear by remember { mutableIntStateOf(0) }
    var birthMonth by remember { mutableIntStateOf(0) }
    var birthDay by remember { mutableIntStateOf(0) }

    val hasBirthDate = birthYear > 0 && birthMonth > 0 && birthDay > 0
    val canContinue = selectedSex != null && selectedGoal != null && hasBirthDate

    val today = remember { Calendar.getInstance() }

    val dateText = if (hasBirthDate) {
        "%02d/%02d/%04d".format(birthDay, birthMonth, birthYear)
    } else {
        "Seleccionar fecha"
    }

    fun openDatePicker() {
        val initialYear = if (birthYear > 0) birthYear else today.get(Calendar.YEAR) - 20
        val initialMonth = if (birthMonth > 0) birthMonth - 1 else today.get(Calendar.MONTH)
        val initialDay = if (birthDay > 0) birthDay else today.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                birthYear = year
                birthMonth = month + 1
                birthDay = dayOfMonth
            },
            initialYear,
            initialMonth,
            initialDay
        ).show()
    }

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
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        HeroSetupCard()

        SectionCard(title = "Sexo") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UserSex.entries.forEach { sex ->
                    SelectionCard(
                        selected = selectedSex == sex,
                        title = sex.label,
                        subtitle = when (sex) {
                            UserSex.MALE -> "Se guardará como dato de perfil."
                            UserSex.FEMALE -> "Se guardará como dato de perfil."
                            UserSex.PREFER_NOT_TO_SAY -> "Podrás usar la app igualmente."
                        },
                        accent = if (selectedSex == sex) Color(0xFF29B6F6) else Color(0xFF4CAF50),
                        onClick = { selectedSex = sex }
                    )
                }
            }
        }

        SectionCard(title = "Fecha de nacimiento") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openDatePicker() },
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasBirthDate) Color(0xFF29B6F6).copy(alpha = 0.55f)
                    else Color.White.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Tu fecha",
                            color = Color.White.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = dateText,
                            color = if (hasBirthDate) Color.White else Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF29B6F6).copy(alpha = 0.18f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📅",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        SectionCard(title = "Objetivo principal") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TrainingGoal.entries.forEach { goal ->
                    val accent = when (goal) {
                        TrainingGoal.GAIN_MUSCLE -> Color(0xFFFFB300)
                        TrainingGoal.LIGHT_EXERCISE -> Color(0xFF4CAF50)
                        TrainingGoal.LOSE_WEIGHT -> Color(0xFFFF7043)
                        TrainingGoal.GENERAL_FITNESS -> Color(0xFF29B6F6)
                    }

                    SelectionCard(
                        selected = selectedGoal == goal,
                        title = goal.label,
                        subtitle = goal.description,
                        accent = accent,
                        onClick = { selectedGoal = goal }
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.24f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Qué cambia con esto",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                MiniInfoRow(
                    accent = Color(0xFF29B6F6),
                    title = "Ejercicios",
                    text = "Seguirán siendo los mismos."
                )

                MiniInfoRow(
                    accent = Color(0xFFFFB300),
                    title = "Retos diarios",
                    text = "Se adaptarán a tu objetivo."
                )

                MiniInfoRow(
                    accent = Color(0xFF4CAF50),
                    title = "Perfil",
                    text = "Guardará tu configuración inicial."
                )
            }
        }

        Button(
            onClick = {
                UserSetupManager.saveUserSetup(
                    context = context,
                    sex = selectedSex!!,
                    birthYear = birthYear,
                    birthMonth = birthMonth,
                    birthDay = birthDay,
                    goal = selectedGoal!!
                )

                navController.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    launchSingleTop = true
                }
            },
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.35f),
                disabledContentColor = Color.Black.copy(alpha = 0.55f)
            )
        ) {
            Text(
                text = "Continuar",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun HeroSetupCard() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.24f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF29B6F6).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Configuración inicial",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color(0xFF29B6F6),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = "Bienvenido a SensAI",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Configura tu perfil antes de empezar. Los ejercicios serán los mismos, pero los retos y objetivos diarios se adaptarán a tu meta.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroStatChip(
                    accent = Color(0xFFFFB300),
                    title = "Reto",
                    value = "Dinámico"
                )
                HeroStatChip(
                    accent = Color(0xFF4CAF50),
                    title = "Perfil",
                    value = "Personal"
                )
                HeroStatChip(
                    accent = Color(0xFF29B6F6),
                    title = "Inicio",
                    value = "1 vez"
                )
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    accent: Color,
    title: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.14f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = value,
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.24f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            content()
        }
    }
}

@Composable
private fun SelectionCard(
    selected: Boolean,
    title: String,
    subtitle: String?,
    accent: Color,
    onClick: () -> Unit
) {
    val background = if (selected) {
        accent.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }

    val border = if (selected) {
        accent.copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(14.dp),
                shape = CircleShape,
                color = accent
            ) {}

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (selected) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "Activo",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniInfoRow(
    accent: Color,
    title: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(10.dp),
            shape = CircleShape,
            color = accent
        ) {}

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}