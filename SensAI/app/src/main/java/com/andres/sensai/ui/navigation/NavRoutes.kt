package com.andres.sensai.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val GOALS = "goals"

    const val TRAINING_MENU_WITH_EXERCISE = "training_menu/{exercise}"
    const val TUTORIAL_CAMERA_WITH_EXERCISE = "tutorial_camera/{exercise}"
    const val TRAINING_CAMERA_WITH_EXERCISE = "training_camera/{exercise}"

    const val SUMMARY = "summary"

    fun trainingMenu(exercise: String) = "training_menu/$exercise"
    fun tutorialCamera(exercise: String) = "tutorial_camera/$exercise"
    fun trainingCamera(exercise: String) = "training_camera/$exercise"
}
