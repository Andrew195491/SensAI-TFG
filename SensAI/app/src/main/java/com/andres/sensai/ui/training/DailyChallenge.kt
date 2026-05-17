package com.andres.sensai.ui.training

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andres.sensai.ui.profile.ProfileManager
import com.andres.sensai.ui.onboarding.TrainingGoal
import com.andres.sensai.ui.onboarding.UserSetupManager
import com.andres.sensai.ui.onboarding.UserSex
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

enum class ExerciseType(
    val id: String,
    val singularName: String,
    val pluralName: String
) {
    SQUAT(
        id = "squat",
        singularName = "sentadilla",
        pluralName = "sentadillas"
    ),
    PUSHUP(
        id = "pushup",
        singularName = "flexión",
        pluralName = "flexiones"
    )
}

data class DailyChallenge(
    val dateKey: String,
    val exerciseType: ExerciseType,
    val targetReps: Int
)

object DailyChallengeManager {

    private const val PREFS_NAME = "sensai_daily_challenges"
    private const val KEY_DEV_DAY_OFFSET = "dev_day_offset"

    fun resetAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun getTodayChallenge(
        context: Context,
        exerciseType: ExerciseType
    ): DailyChallenge {
        val today = todayKey(context)

        val setup = UserSetupManager.getUserSetup(context)
        val goal = setup?.goal ?: TrainingGoal.GENERAL_FITNESS
        val sex = setup?.sex ?: UserSex.PREFER_NOT_TO_SAY
        val age = UserSetupManager.getAge(context)

        val seed = listOf(
            today,
            exerciseType.name,
            goal.id,
            sex.id,
            age?.toString() ?: "na"
        ).joinToString("|").hashCode()

        val random = Random(seed)

        val baseTarget = baseTargetsForGoal(goal, exerciseType).random(random)

        val finalTarget = adjustTargetForProfile(
            baseTarget = baseTarget,
            exerciseType = exerciseType,
            age = age,
            sex = sex
        )

        return DailyChallenge(
            dateKey = today,
            exerciseType = exerciseType,
            targetReps = finalTarget
        )
    }

    private fun baseTargetsForGoal(
        goal: TrainingGoal,
        exerciseType: ExerciseType
    ): List<Int> {
        return when (goal) {
            TrainingGoal.GAIN_MUSCLE -> {
                when (exerciseType) {
                    ExerciseType.SQUAT -> listOf(16, 18, 20, 22)
                    ExerciseType.PUSHUP -> listOf(8, 10, 12, 14)
                }
            }

            TrainingGoal.LIGHT_EXERCISE -> {
                when (exerciseType) {
                    ExerciseType.SQUAT -> listOf(8, 10, 12, 14)
                    ExerciseType.PUSHUP -> listOf(4, 5, 6, 8)
                }
            }

            TrainingGoal.LOSE_WEIGHT -> {
                when (exerciseType) {
                    ExerciseType.SQUAT -> listOf(18, 22, 26, 30)
                    ExerciseType.PUSHUP -> listOf(8, 10, 12, 15)
                }
            }

            TrainingGoal.GENERAL_FITNESS -> {
                when (exerciseType) {
                    ExerciseType.SQUAT -> listOf(12, 15, 18, 20)
                    ExerciseType.PUSHUP -> listOf(6, 8, 10, 12)
                }
            }
        }
    }

    private fun adjustTargetForProfile(
        baseTarget: Int,
        exerciseType: ExerciseType,
        age: Int?,
        sex: UserSex
    ): Int {
        val ageFactor = ageFactor(age)
        val sexFactor = sexFactor(sex, exerciseType)

        val adjusted = (baseTarget * ageFactor * sexFactor).roundToInt()

        return adjusted.coerceIn(
            minimumTarget(exerciseType),
            maximumTarget(exerciseType)
        )
    }

    private fun ageFactor(age: Int?): Float {
        if (age == null) return 1.00f

        return when {
            age <= 15 -> 0.70f
            age in 16..17 -> 0.82f
            age in 18..29 -> 1.00f
            age in 30..44 -> 0.96f
            age in 45..59 -> 0.88f
            else -> 0.78f
        }
    }

    private fun sexFactor(
        sex: UserSex,
        exerciseType: ExerciseType
    ): Float {
        return when (sex) {
            UserSex.MALE -> 1.00f

            UserSex.FEMALE -> {
                when (exerciseType) {
                    ExerciseType.SQUAT -> 0.97f
                    ExerciseType.PUSHUP -> 0.92f
                }
            }

            UserSex.PREFER_NOT_TO_SAY -> 1.00f
        }
    }

    private fun minimumTarget(exerciseType: ExerciseType): Int {
        return when (exerciseType) {
            ExerciseType.SQUAT -> 6
            ExerciseType.PUSHUP -> 3
        }
    }

    private fun maximumTarget(exerciseType: ExerciseType): Int {
        return when (exerciseType) {
            ExerciseType.SQUAT -> 35
            ExerciseType.PUSHUP -> 20
        }
    }

    fun getProgress(context: Context, challenge: DailyChallenge): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(progressKey(challenge), 0)
    }

    fun addProgress(
        context: Context,
        challenge: DailyChallenge,
        delta: Int
    ): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(progressKey(challenge), 0)
        val updated = (current + delta).coerceAtLeast(0)

        prefs.edit()
            .putInt(progressKey(challenge), updated)
            .apply()

        return updated
    }

    fun isCompleted(context: Context, challenge: DailyChallenge): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(completionKey(challenge), false)
    }

    fun completeChallenge(
        context: Context,
        challenge: DailyChallenge
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyCompleted = prefs.getBoolean(completionKey(challenge), false)

        if (alreadyCompleted) return false

        prefs.edit()
            .putBoolean(completionKey(challenge), true)
            .apply()

        ProfileManager.rewardDailyChallenge(context, challenge.exerciseType)
        return true
    }

    fun advanceDeveloperDay(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentOffset = prefs.getInt(KEY_DEV_DAY_OFFSET, 0)
        prefs.edit()
            .putInt(KEY_DEV_DAY_OFFSET, currentOffset + 1)
            .apply()
        return todayKey(context)
    }

    fun resetDeveloperDay(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_DEV_DAY_OFFSET, 0)
            .apply()
        return todayKey(context)
    }

    fun getDeveloperDayOffset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DEV_DAY_OFFSET, 0)
    }

    fun getCurrentDayLabel(context: Context): String {
        return todayKey(context)
    }

    private fun progressKey(challenge: DailyChallenge): String {
        return "progress_${challenge.dateKey}_${challenge.exerciseType.id}"
    }

    private fun completionKey(challenge: DailyChallenge): String {
        return "completed_${challenge.dateKey}_${challenge.exerciseType.id}"
    }

    private fun todayKey(context: Context): String {
        val calendar = Calendar.getInstance()
        val offset = getDeveloperDayOffset(context)
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(calendar.time)
    }
}

@Composable
fun DailyChallengeCompactInfo(
    challenge: DailyChallenge,
    progress: Int,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val safeProgress = progress.coerceAtMost(challenge.targetReps)
    val progressFraction =
        if (challenge.targetReps > 0) safeProgress.toFloat() / challenge.targetReps.toFloat()
        else 0f

    val accent = if (completed) Color(0xFF4CAF50) else Color(0xFFFFB300)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Reto diario",
                    tint = accent
                )

                Text(
                    text = "Reto diario",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.18f)
            ) {
                Text(
                    text = "$safeProgress/${challenge.targetReps}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Text(
                text = if (completed) "Completado hoy" else "Sigue así",
                color = if (completed) accent else Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}