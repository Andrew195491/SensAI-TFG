package com.andres.sensai.ui.screens

import androidx.compose.foundation.layout.aspectRatio // Controla la ASPECT RATIO de los objetos
import androidx.compose.foundation.layout.Arrangement // Controla el ORDEN de los objetos
import androidx.compose.foundation.layout.Column // Controla las COLUMNAS de los objetos
import androidx.compose.foundation.layout.PaddingValues // Controla el VALOR DEL PADDING
import androidx.compose.foundation.layout.fillMaxSize // Controla el TAMAÑO de los objetos
import androidx.compose.foundation.layout.fillMaxWidth // Controla el ANCHO de los objetos
import androidx.compose.foundation.BorderStroke // Controla el BORDE de los objetos
import androidx.compose.ui.graphics.Color // Controla el COLOR de los objetos
import androidx.compose.material3.OutlinedButton // Permite botones sin relleno
import androidx.compose.material3.ButtonDefaults // Controla los ATRIBUTOS de los botones
import androidx.compose.material3.CardDefaults // Controla los ATRIBUTOS de las tarjetas
import androidx.compose.foundation.clickable // Permite que los objetos sean CLICKABLES
import androidx.compose.foundation.layout.padding // Controla el PADDING de los objetos
import androidx.compose.foundation.lazy.grid.GridCells // Controla el GRID de los objetos
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid // Controla el GRID de los objetos
import androidx.compose.foundation.shape.RoundedCornerShape // Controla el FORMA de los objetos
import androidx.compose.material.icons.Icons // Controla los ICONOS
import androidx.compose.material.icons.filled.Face // Permite poner una cara de icono
import androidx.compose.material3.Button // Permite botones
import androidx.compose.material3.Card // Permite tarjetas
import androidx.compose.material3.HorizontalDivider // Permite líneas separatorias
import androidx.compose.material3.Icon // Permite iconos
import androidx.compose.material3.MaterialTheme // Controla el TEMA de los objetos
import androidx.compose.material3.Text // Permite texto
import androidx.compose.runtime.Composable // Permite que los objetos sean COMPUESTOS
import androidx.compose.ui.Alignment // Controla la POSICIÓN de los objetos
import androidx.compose.ui.Modifier // Permite modificar los objetos
import androidx.compose.ui.unit.dp // Controla el TAMAÑO de los objetos
import androidx.navigation.NavController // Permite navegar entre pantallas
import com.andres.sensai.ui.navigation.NavRoutes // Permite navegar entre pantallas


@Composable
fun HomeScreen(navController: NavController) {
    val exercises = listOf("sentadilla", "flexion")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PERFIL (card con borde + fondo sutil, sin efecto "botón coloreado")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(NavRoutes.PROFILE) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Perfil",
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Nombre del usuario",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Nivel 1",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // LINEA SEPARATORIA (con aire)
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 4.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // GRID DE EJERCICIOS
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp)
        ) {
            items(exercises.size) { index ->
                OutlinedButton(
                    onClick = { navController.navigate(NavRoutes.trainingMenu(exercises[index])) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f), // cuadrado
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text(
                        text = exercises[index].replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // OBJETIVOS (un poco más elegante)
        Button(
            onClick = { navController.navigate(NavRoutes.GOALS) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Objetivos", style = MaterialTheme.typography.titleMedium)
        }
    }
}
