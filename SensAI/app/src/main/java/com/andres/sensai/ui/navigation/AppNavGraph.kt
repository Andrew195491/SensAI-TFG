package com.andres.sensai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andres.sensai.ui.home.HomeScreen
import com.andres.sensai.ui.profile.ProfileScreen
import com.andres.sensai.ui.training.TrainScreen
import com.andres.sensai.ui.tutorial.TutorialScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }

        composable(
            route = NavRoutes.TUTORIAL,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "squat"
            TutorialScreen(navController = navController, exerciseId = exerciseId)
        }

        // Entrenar solo sentadilla
        composable(NavRoutes.TRAIN_SQUAT) {
            TrainScreen(navController = navController)
        }

    }
}