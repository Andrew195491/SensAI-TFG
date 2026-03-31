package com.andres.sensai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
<<<<<<< HEAD
<<<<<<< HEAD
import com.andres.sensai.ui.navigation.AppNavGraph
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
=======
=======
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.andres.sensai.ui.navigation.SensAiNavHost
import com.andres.sensai.ui.theme.SensAITheme
<<<<<<< HEAD
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
=======
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
<<<<<<< HEAD
<<<<<<< HEAD
            MaterialTheme {
                Surface {
                    AppNavGraph()
                }
            }
=======
=======
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
            SensAiApp()
        }
    }
}

@Composable
fun SensAiApp() {
    SensAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            SensAiNavHost(navController = navController)
<<<<<<< HEAD
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
=======
>>>>>>> parent of 3ebc7e3 (Implementación inicial IA + Ventana ordenada HOME + retroceso en PERFIL y OBJETIVOS)
        }
    }
}
