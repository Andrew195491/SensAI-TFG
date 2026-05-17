package com.andres.sensai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andres.sensai.ui.home.HomeScreen
import com.andres.sensai.ui.onboarding.OnboardingScreen
import com.andres.sensai.ui.onboarding.UserSetupManager
import com.andres.sensai.ui.profile.ProfileScreen
import com.andres.sensai.ui.training.ExerciseType
import com.andres.sensai.ui.training.TrainScreen
import com.andres.sensai.ui.tutorial.TutorialScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    val context = LocalContext.current

    val startDestination = if (UserSetupManager.isCompleted(context)) {
        NavRoutes.HOME
    } else {
        NavRoutes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(navController = navController)
        }

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

        composable(NavRoutes.TRAIN_SQUAT) {
            TrainScreen(
                navController = navController,
                exerciseType = ExerciseType.SQUAT
            )
        }

        composable(NavRoutes.TRAIN_PUSHUP) {
            TrainScreen(
                navController = navController,
                exerciseType = ExerciseType.PUSHUP
            )
        }
    }
}