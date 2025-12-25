package com.andres.sensai.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Calibration : Screen("calibration")
    object Workout : Screen("workout")
    object Summary : Screen("summary")
}
