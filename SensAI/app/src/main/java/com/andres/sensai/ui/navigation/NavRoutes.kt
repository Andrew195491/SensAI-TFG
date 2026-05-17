package com.andres.sensai.ui.navigation

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PROFILE = "profile"

    const val TUTORIAL = "tutorial/{exerciseId}"
    fun tutorial(exerciseId: String) = "tutorial/$exerciseId"

    const val TRAIN_SQUAT = "train/squat"
    const val TRAIN_PUSHUP = "train/pushup"
}