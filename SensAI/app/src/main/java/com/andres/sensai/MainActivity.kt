package com.andres.sensai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andres.sensai.ui.navigation.NavRoutes
import com.andres.sensai.ui.screens.*
import com.andres.sensai.ui.theme.SensAITheme
import com.andres.sensai.ui.training.TrainingCameraScreen
import com.andres.sensai.ui.training.TutorialCameraScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensAITheme {
                CoachCamApp()
            }
        }
    }
}

@Composable
fun CoachCamApp() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(navController = navController)
            }

            composable(NavRoutes.PROFILE) {
                ProfileScreen(navController = navController)
            }

            composable(NavRoutes.GOALS) {
                GoalsScreen(navController = navController)
            }

            composable(NavRoutes.TRAINING_MENU_WITH_EXERCISE) { backStackEntry ->
                val exercise = backStackEntry.arguments?.getString("exercise") ?: "sentadilla"
                TrainingMenuScreen(navController = navController, exercise = exercise)
            }


            composable(NavRoutes.TUTORIAL_CAMERA_WITH_EXERCISE) { backStackEntry ->
                val exercise = backStackEntry.arguments?.getString("exercise") ?: "sentadilla"
                TutorialCameraScreen(navController = navController, exercise = exercise)
            }

            composable(NavRoutes.TRAINING_CAMERA_WITH_EXERCISE) { backStackEntry ->
                val exercise = backStackEntry.arguments?.getString("exercise") ?: "sentadilla"
                TrainingCameraScreen(
                    navController = navController,
                    exercise = exercise,
                    onFinish = { navController.navigate(NavRoutes.SUMMARY) }
                )
            }


            composable(NavRoutes.SUMMARY) {
                SummaryScreen(
                    onFinish = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
