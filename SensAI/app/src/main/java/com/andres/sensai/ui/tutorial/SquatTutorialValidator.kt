package com.andres.sensai.ui.tutorial

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class SquatTutorialValidator(
    private val stableRequiredMs: Long = 700L,
    private val minVisibility: Float = 0.35f
) : TutorialValidator {

    private var phase: TutorialPhase = TutorialPhase.READY
    private var correctSinceMs: Long = 0L

    override fun reset() {
        phase = TutorialPhase.READY
        correctSinceMs = 0L
    }

    override fun update(
        landmarks: List<NormalizedLandmark>,
        nowMs: Long
    ): TutorialFeedback {
        if (phase == TutorialPhase.DONE) {
            return TutorialFeedback(
                phase = TutorialPhase.DONE,
                isCorrect = true,
                title = "Tutorial completado",
                instruction = "Ya puedes empezar la sentadilla.",
                correction = "",
                stableMs = stableRequiredMs
            )
        }

        if (landmarks.size < 33) {
            correctSinceMs = 0L
            return feedback(
                phase = phase,
                correct = false,
                stableMs = 0L,
                correction = "Colócate en cámara y deja visibles piernas y cadera."
            )
        }

        val feat = computeFeatures(landmarks)
        if (!feat.isReliable) {
            correctSinceMs = 0L
            return feedback(
                phase = phase,
                correct = false,
                stableMs = 0L,
                correction = "Ponte de frente o semi-frontal y deja el cuerpo bien visible."
            )
        }

        val isCorrect = when (phase) {
            TutorialPhase.READY -> feat.kneeAngleAvg >= 155f
            TutorialPhase.DOWN -> feat.kneeAngleAvg in 110f..150f
            TutorialPhase.BOTTOM -> feat.kneeAngleAvg <= 105f
            TutorialPhase.UP -> feat.kneeAngleAvg >= 155f
            TutorialPhase.DONE -> true
        }

        val stableMs = updateStableTime(isCorrect, nowMs)

        if (isCorrect && stableMs >= stableRequiredMs) {
            phase = nextPhase(phase)
            correctSinceMs = 0L

            if (phase == TutorialPhase.DONE) {
                return TutorialFeedback(
                    phase = TutorialPhase.DONE,
                    isCorrect = true,
                    title = "Tutorial completado",
                    instruction = "Movimiento correcto.",
                    correction = "",
                    stableMs = stableRequiredMs
                )
            }
        }

        return feedback(
            phase = phase,
            correct = isCorrect,
            stableMs = stableMs,
            correction = correctionForPhase(phase)
        )
    }

    private fun nextPhase(current: TutorialPhase): TutorialPhase {
        return when (current) {
            TutorialPhase.READY -> TutorialPhase.DOWN
            TutorialPhase.DOWN -> TutorialPhase.BOTTOM
            TutorialPhase.BOTTOM -> TutorialPhase.UP
            TutorialPhase.UP -> TutorialPhase.DONE
            TutorialPhase.DONE -> TutorialPhase.DONE
        }
    }

    private fun updateStableTime(correct: Boolean, nowMs: Long): Long {
        if (!correct) {
            correctSinceMs = 0L
            return 0L
        }

        if (correctSinceMs == 0L) {
            correctSinceMs = nowMs
            return 0L
        }

        return nowMs - correctSinceMs
    }

    private fun feedback(
        phase: TutorialPhase,
        correct: Boolean,
        stableMs: Long,
        correction: String
    ): TutorialFeedback {
        return when (phase) {
            TutorialPhase.READY -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 1 · Inicio",
                instruction = "Colócate arriba, estable y de frente.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.DOWN -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 2 · Bajada",
                instruction = "Empieza a bajar con control.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.BOTTOM -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 3 · Abajo",
                instruction = "Llega a la parte baja y mantén un instante.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.UP -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 4 · Subida",
                instruction = "Sube hasta volver completamente arriba.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.DONE -> TutorialFeedback(
                phase = phase,
                isCorrect = true,
                title = "Tutorial completado",
                instruction = "Movimiento correcto.",
                correction = "",
                stableMs = stableMs
            )
        }
    }

    private fun correctionForPhase(phase: TutorialPhase): String {
        return when (phase) {
            TutorialPhase.READY -> "Ponte recto y estable."
            TutorialPhase.DOWN -> "Baja más y mantén el control."
            TutorialPhase.BOTTOM -> "Aún no has llegado abajo del todo."
            TutorialPhase.UP -> "Termina de subir completamente."
            TutorialPhase.DONE -> ""
        }
    }

    private data class Features(
        val isReliable: Boolean,
        val kneeAngleAvg: Float
    )

    private fun computeFeatures(lm: List<NormalizedLandmark>): Features {
        val lHip = lm[23]
        val rHip = lm[24]
        val lKnee = lm[25]
        val rKnee = lm[26]
        val lAnkle = lm[27]
        val rAnkle = lm[28]
        val lShoulder = lm[11]
        val rShoulder = lm[12]

        val visOk =
            visibilityOf(lHip) >= minVisibility &&
                    visibilityOf(rHip) >= minVisibility &&
                    visibilityOf(lKnee) >= minVisibility &&
                    visibilityOf(rKnee) >= minVisibility &&
                    visibilityOf(lAnkle) >= minVisibility &&
                    visibilityOf(rAnkle) >= minVisibility &&
                    visibilityOf(lShoulder) >= minVisibility &&
                    visibilityOf(rShoulder) >= minVisibility

        if (!visOk) return Features(false, 180f)

        val leftKneeAngle = angleDeg(lHip, lKnee, lAnkle)
        val rightKneeAngle = angleDeg(rHip, rKnee, rAnkle)
        val avg = (leftKneeAngle + rightKneeAngle) / 2f

        return Features(true, avg)
    }

    private fun visibilityOf(lm: NormalizedLandmark): Float {
        return try {
            lm.visibility().orElse(1f)
        } catch (_: Throwable) {
            1f
        }
    }

    private fun angleDeg(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark
    ): Float {
        val abx = a.x() - b.x()
        val aby = a.y() - b.y()
        val cbx = c.x() - b.x()
        val cby = c.y() - b.y()

        val dot = abx * cbx + aby * cby
        val abNorm = sqrt(abx * abx + aby * aby).coerceAtLeast(1e-6f)
        val cbNorm = sqrt(cbx * cbx + cby * cby).coerceAtLeast(1e-6f)

        val cosTheta = (dot / (abNorm * cbNorm)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
    }
}