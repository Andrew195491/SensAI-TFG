package com.andres.sensai.ui.tutorial

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

enum class TutorialPhase {
    READY,
    DOWN,
    BOTTOM,
    UP,
    DONE
}

data class TutorialFeedback(
    val phase: TutorialPhase,
    val isCorrect: Boolean,
    val title: String,
    val instruction: String,
    val correction: String,
    val stableMs: Long
)

interface TutorialValidator {
    fun reset()

    fun update(
        landmarks: List<NormalizedLandmark>,
        nowMs: Long
    ): TutorialFeedback
}