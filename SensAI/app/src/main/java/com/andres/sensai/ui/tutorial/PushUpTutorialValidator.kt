package com.andres.sensai.ui.tutorial

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpTutorialValidator(
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
                instruction = "Ya puedes empezar la flexión.",
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
                correction = "Colócate de perfil y deja visible el cuerpo."
            )
        }

        val feat = computeFeatures(landmarks)
        if (!feat.isReliable) {
            correctSinceMs = 0L
            return feedback(
                phase = phase,
                correct = false,
                stableMs = 0L,
                correction = "Ponte de lado y deja visibles hombro, codo y tobillo."
            )
        }

        val isCorrect = when (phase) {
            TutorialPhase.READY -> feat.elbowAngle >= 155f && feat.bodyLine >= 0.60f
            TutorialPhase.DOWN -> feat.elbowAngle in 100f..145f && feat.bodyLine >= 0.55f
            TutorialPhase.BOTTOM -> feat.elbowAngle <= 95f && feat.bodyLine >= 0.50f
            TutorialPhase.UP -> feat.elbowAngle >= 155f && feat.bodyLine >= 0.55f
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
                instruction = "Colócate arriba con el cuerpo alineado.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.DOWN -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 2 · Bajada",
                instruction = "Flexiona los codos y baja con control.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.BOTTOM -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 3 · Abajo",
                instruction = "Llega abajo y mantén la posición.",
                correction = correction,
                stableMs = stableMs
            )

            TutorialPhase.UP -> TutorialFeedback(
                phase = phase,
                isCorrect = correct,
                title = "Fase 4 · Subida",
                instruction = "Empuja hasta volver arriba.",
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
            TutorialPhase.READY -> "Mantén el cuerpo recto y estable."
            TutorialPhase.DOWN -> "Baja más sin doblar el cuerpo."
            TutorialPhase.BOTTOM -> "Aún no has llegado abajo del todo."
            TutorialPhase.UP -> "Empuja hasta extender los brazos."
            TutorialPhase.DONE -> ""
        }
    }

    private data class Features(
        val isReliable: Boolean,
        val elbowAngle: Float,
        val bodyLine: Float
    )

    private fun computeFeatures(lm: List<NormalizedLandmark>): Features {
        val leftScore = visibilityScore(lm[11], lm[13], lm[15], lm[23], lm[27])
        val rightScore = visibilityScore(lm[12], lm[14], lm[16], lm[24], lm[28])

        val useLeft = leftScore >= rightScore

        val shoulder = if (useLeft) lm[11] else lm[12]
        val elbow = if (useLeft) lm[13] else lm[14]
        val wrist = if (useLeft) lm[15] else lm[16]
        val hip = if (useLeft) lm[23] else lm[24]
        val ankle = if (useLeft) lm[27] else lm[28]

        val visOk =
            visibilityOf(shoulder) >= minVisibility &&
                    visibilityOf(elbow) >= minVisibility &&
                    visibilityOf(wrist) >= minVisibility &&
                    visibilityOf(hip) >= minVisibility &&
                    visibilityOf(ankle) >= minVisibility

        if (!visOk) return Features(false, 180f, 0f)

        val elbowAngle = angleDeg(shoulder, elbow, wrist)
        val hipAngle = angleDeg(shoulder, hip, ankle)
        val bodyLine = normalize(hipAngle, 145f, 180f)

        return Features(true, elbowAngle, bodyLine)
    }

    private fun visibilityScore(
        a: NormalizedLandmark,
        b: NormalizedLandmark,
        c: NormalizedLandmark,
        d: NormalizedLandmark,
        e: NormalizedLandmark
    ): Float {
        return visibilityOf(a) + visibilityOf(b) + visibilityOf(c) + visibilityOf(d) + visibilityOf(e)
    }

    private fun visibilityOf(lm: NormalizedLandmark): Float {
        return try {
            lm.visibility().orElse(1f)
        } catch (_: Throwable) {
            1f
        }
    }

    private fun normalize(value: Float, minVal: Float, maxVal: Float): Float {
        if (maxVal <= minVal) return 0f
        return ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
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