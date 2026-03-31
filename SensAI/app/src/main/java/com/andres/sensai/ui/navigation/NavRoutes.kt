package com.andres.sensai.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val PROFILE = "profile"

    // tutorial por ejercicio
    const val TUTORIAL = "tutorial/{exerciseId}"
    fun tutorial(exerciseId: String) = "tutorial/$exerciseId"

    // entreno por ejercicio
    const val TRAIN_SQUAT = "train/squat"
    const val TRAIN_PUSHUP = "train/pushup"
}