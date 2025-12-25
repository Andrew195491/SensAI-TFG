package com.andres.sensai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.andres.sensai.ui.navigation.SensAiNavHost
import com.andres.sensai.ui.theme.SensAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
        }
    }
}
