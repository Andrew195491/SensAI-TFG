package com.andres.sensai.ui.onboarding

import android.content.Context
import java.util.Calendar

object UserSetupManager {

    private const val PREFS_NAME = "sensai_user_setup"

    private const val KEY_COMPLETED = "completed"
    private const val KEY_SEX = "sex"
    private const val KEY_BIRTH_YEAR = "birth_year"
    private const val KEY_BIRTH_MONTH = "birth_month"
    private const val KEY_BIRTH_DAY = "birth_day"
    private const val KEY_GOAL = "goal"

    fun isCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun saveUserSetup(
        context: Context,
        sex: UserSex,
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        goal: TrainingGoal
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_COMPLETED, true)
            .putString(KEY_SEX, sex.id)
            .putInt(KEY_BIRTH_YEAR, birthYear)
            .putInt(KEY_BIRTH_MONTH, birthMonth)
            .putInt(KEY_BIRTH_DAY, birthDay)
            .putString(KEY_GOAL, goal.id)
            .apply()
    }

    fun getUserSetup(context: Context): UserSetup? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_COMPLETED, false)) return null

        val year = prefs.getInt(KEY_BIRTH_YEAR, -1)
        val month = prefs.getInt(KEY_BIRTH_MONTH, -1)
        val day = prefs.getInt(KEY_BIRTH_DAY, -1)

        if (year <= 0 || month <= 0 || day <= 0) return null

        return UserSetup(
            sex = UserSex.fromId(prefs.getString(KEY_SEX, null)),
            birthYear = year,
            birthMonth = month,
            birthDay = day,
            goal = TrainingGoal.fromId(prefs.getString(KEY_GOAL, null))
        )
    }

    fun getAge(context: Context): Int? {
        val setup = getUserSetup(context) ?: return null

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - setup.birthYear

        val currentMonth = today.get(Calendar.MONTH) + 1
        val currentDay = today.get(Calendar.DAY_OF_MONTH)

        if (
            currentMonth < setup.birthMonth ||
            (currentMonth == setup.birthMonth && currentDay < setup.birthDay)
        ) {
            age -= 1
        }

        return age.coerceAtLeast(0)
    }

    fun reset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}