package com.andres.sensai.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.ui.navigation.NavRoutes

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "SensAI",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Elige un ejercicio:",
            style = MaterialTheme.typography.titleMedium
        )

        // =========================
        // SENTADILLA
        // =========================
        Card(
            colors = CardDefaults.cardColors(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sentadilla",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Cuenta repeticiones usando detección de pose en tiempo real.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { navController.navigate(NavRoutes.TRAIN_SQUAT) }
                    ) {
                        Text("Entrenar")
                    }

                    OutlinedButton(
                        onClick = { navController.navigate(NavRoutes.tutorial("squat")) }
                    ) {
                        Text("Tutorial")
                    }
                }
            }
        }

        // =========================
        // FLEXIONES
        // =========================
        Card(
            colors = CardDefaults.cardColors(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Flexiones",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Cuenta repeticiones de flexiones con el mismo sistema.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { navController.navigate(NavRoutes.TRAIN_PUSHUP) }
                    ) {
                        Text("Entrenar")
                    }

                    OutlinedButton(
                        onClick = { navController.navigate(NavRoutes.tutorial("pushup")) }
                    ) {
                        Text("Tutorial")
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        OutlinedButton(onClick = { navController.navigate(NavRoutes.PROFILE) }) {
            Text("Perfil")
        }
    }
}