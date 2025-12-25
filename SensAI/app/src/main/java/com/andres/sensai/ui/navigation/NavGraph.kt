package com.andres.sensai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.andres.sensai.ui.screens.*

@Composable
fun SensAiNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(Screen.Calibration.route)
                }
            )
        }

        composable(Screen.Calibration.route) {
            CalibrationScreen(
                onStartWorkout = {
                    navController.navigate(Screen.Workout.route)
                }
            )
        }

        composable(Screen.Workout.route) {
            WorkoutScreen(
                onFinishWorkout = {
                    navController.navigate(Screen.Summary.route)
                }
            )
        }

        composable(Screen.Summary.route) {
            SummaryScreen(
                onFinish = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
