package com.andres.sensai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.andres.sensai.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingMenuScreen(
    navController: NavController,
    exercise: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Entrenamiento: ${exercise.replaceFirstChar { it.uppercaseChar() }}")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate(NavRoutes.tutorialCamera(exercise)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tutorial") }

            Button(
                onClick = { navController.navigate(NavRoutes.trainingCamera(exercise)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Entrenar") }
        }
    }
}
