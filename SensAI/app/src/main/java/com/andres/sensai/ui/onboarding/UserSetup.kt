package com.andres.sensai.ui.onboarding

enum class UserSex(val id: String, val label: String) {
    MALE("male", "Hombre"),
    FEMALE("female", "Mujer"),
    PREFER_NOT_TO_SAY("na", "Prefiero no decirlo");

    companion object {
        fun fromId(id: String?): UserSex {
            return entries.firstOrNull { it.id == id } ?: PREFER_NOT_TO_SAY
        }
    }
}

enum class TrainingGoal(
    val id: String,
    val label: String,
    val shortLabel: String,
    val description: String
) {
    GAIN_MUSCLE(
        id = "gain_muscle",
        label = "Ganar masa muscular",
        shortLabel = "Músculo",
        description = "Objetivos con un poco más de intensidad y progresión."
    ),
    LIGHT_EXERCISE(
        id = "light_exercise",
        label = "Ejercicio suave",
        shortLabel = "Suave",
        description = "Objetivos más llevaderos para empezar o mantener rutina."
    ),
    LOSE_WEIGHT(
        id = "lose_weight",
        label = "Perder grasa",
        shortLabel = "Definición",
        description = "Objetivos algo más constantes y con mayor volumen."
    ),
    GENERAL_FITNESS(
        id = "general_fitness",
        label = "Mantenerme en forma",
        shortLabel = "General",
        description = "Equilibrio entre constancia, técnica y progresión."
    );

    companion object {
        fun fromId(id: String?): TrainingGoal {
            return entries.firstOrNull { it.id == id } ?: GENERAL_FITNESS
        }
    }
}

data class UserSetup(
    val sex: UserSex,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val goal: TrainingGoal
)