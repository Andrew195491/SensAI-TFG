package com.andres.sensai.ui.profile

import android.content.Context
import com.andres.sensai.ui.training.ExerciseType

data class ProfileProgress(
    val level: Int,
    val totalXp: Int,
    val xpIntoCurrentLevel: Int,
    val xpForNextLevel: Int,
    val completedDailyChallenges: Int
)

object ProfileManager {

    private const val PREFS_NAME = "sensai_profile"
    private const val KEY_TOTAL_XP = "total_xp"
    private const val KEY_COMPLETED_DAILY = "completed_daily"

    fun resetProfile(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun getDailyChallengeXpReward(exerciseType: ExerciseType): Int {
        return when (exerciseType) {
            ExerciseType.SQUAT -> 50
            ExerciseType.PUSHUP -> 50
        }
    }

    fun rewardDailyChallenge(
        context: Context,
        exerciseType: ExerciseType
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val currentCompleted = prefs.getInt(KEY_COMPLETED_DAILY, 0)

        prefs.edit()
            .putInt(KEY_TOTAL_XP, currentXp + getDailyChallengeXpReward(exerciseType))
            .putInt(KEY_COMPLETED_DAILY, currentCompleted + 1)
            .apply()
    }

    fun getProfileProgress(context: Context): ProfileProgress {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalXp = prefs.getInt(KEY_TOTAL_XP, 0)
        val completedDaily = prefs.getInt(KEY_COMPLETED_DAILY, 0)

        var level = 1
        var remainingXp = totalXp
        var required = xpRequiredForNextLevel(level)

        while (remainingXp >= required) {
            remainingXp -= required
            level++
            required = xpRequiredForNextLevel(level)
        }

        return ProfileProgress(
            level = level,
            totalXp = totalXp,
            xpIntoCurrentLevel = remainingXp,
            xpForNextLevel = required,
            completedDailyChallenges = completedDaily
        )
    }

    private fun xpRequiredForNextLevel(level: Int): Int {
        return 100 + ((level - 1) * 25)
    }
}