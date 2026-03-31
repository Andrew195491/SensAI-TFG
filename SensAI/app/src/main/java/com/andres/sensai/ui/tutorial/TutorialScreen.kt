package com.andres.sensai.ui.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    exerciseId: String
) {
    val title = when (exerciseId) {
        "squat" -> "Tutorial: Sentadilla"
        "pushup" -> "Tutorial: Flexiones"
        else -> "Tutorial"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
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
                .padding(16.dp)
        ) {
            Column {
                when (exerciseId) {
                    "squat" -> {
                        Text("📌 Sentadilla:")
                        Text("1) Pies al ancho de hombros.")
                        Text("2) Baja la cadera manteniendo espalda recta.")
                        Text("3) Rodillas alineadas, vuelve arriba.")
                    }
                    "pushup" -> {
                        Text("📌 Flexiones:")
                        Text("1) Manos bajo hombros, cuerpo recto.")
                        Text("2) Baja doblando codos sin romper la línea del cuerpo.")
                        Text("3) Sube extendiendo brazos.")
                    }
                    else -> {
                        Text("Ejercicio desconocido: $exerciseId")
                    }
                }
            }
        }
    }
}