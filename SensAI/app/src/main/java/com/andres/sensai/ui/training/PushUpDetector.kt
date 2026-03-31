package com.andres.sensai.ui.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpDetector(
    private val upThreshold: Float = 0.70f,
    private val downThreshold: Float = 0.40f,
    private val stableRequiredMs: Long = 180L,
    private val repCooldownMs: Long = 300L,
    private val scoreWindowSize: Int = 6,
    private val minVisibility: Float = 0.35f
) {

    enum class State { WAIT_UP, GOING_DOWN, WAIT_DOWN, GOING_UP }

    data class Debug(
        val rawScore: Float,
        val smoothScore: Float,
        val elbowAngleL: Float,
        val elbowAngleR: Float,
        val stableMs: Long
    )

    data class Output(
        val reps: Int,
        val state: State,
        val score: Float,
        val debug: Debug
    )

    private var state: State = State.WAIT_UP
    private var reps: Int = 0
    private var stateSinceMs: Long = 0L
    private var lastRepAtMs: Long = 0L

    private val scoreBuffer = ArrayDeque<Float>()

    // fallback debug
    private var lastRawScore = 1f
    private var lastSmoothScore = 1f
    private var lastElbowL = 180f
    private var lastElbowR = 180f

    fun reset(nowMs: Long = 0L) {
        state = State.WAIT_UP
        reps = 0
        stateSinceMs = nowMs
        lastRepAtMs = 0L
        scoreBuffer.clear()
    }

    fun update(landmarks: List<NormalizedLandmark>, nowMs: Long): Output {
        if (stateSinceMs == 0L) stateSinceMs = nowMs

        if (landmarks.size < 33) {
            return Output(
                reps, state, lastSmoothScore,
                Debug(lastRawScore, lastSmoothScore, lastElbowL, lastElbowR, nowMs - stateSinceMs)
            )
        }

        val feat = computeFeatures(landmarks)
        if (!feat.isReliable) {
            return Output(
                reps, state, lastSmoothScore,
                Debug(lastRawScore, lastSmoothScore, lastElbowL, lastElbowR, nowMs - stateSinceMs)
            )
        }

        val rawScore = feat.upScore
        val smoothScore = smooth(rawScore)

        val stableMs = nowMs - stateSinceMs
        val cooldownOk = (nowMs - lastRepAtMs) >= repCooldownMs

        when (state) {
            State.WAIT_UP -> {
                if (smoothScore < upThreshold) {
                    state = State.GOING_DOWN
                    stateSinceMs = nowMs
                }
            }

            State.GOING_DOWN -> {
                if (smoothScore >= upThreshold) {
                    state = State.WAIT_UP
                    stateSinceMs = nowMs
                } else if (smoothScore <= downThreshold && stableMs >= stableRequiredMs) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs
                }
            }

            State.WAIT_DOWN -> {
                if (smoothScore > downThreshold) {
                    state = State.GOING_UP
                    stateSinceMs = nowMs
                }
            }

            State.GOING_UP -> {
                if (smoothScore <= downThreshold) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs
                } else if (smoothScore >= upThreshold && stableMs >= stableRequiredMs && cooldownOk) {
                    reps += 1
                    lastRepAtMs = nowMs
                    state = State.WAIT_UP
                    stateSinceMs = nowMs
                }
            }
        }

        lastRawScore = rawScore
        lastSmoothScore = smoothScore
        lastElbowL = feat.elbowL
        lastElbowR = feat.elbowR

        return Output(
            reps = reps,
            state = state,
            score = smoothScore,
            debug = Debug(
                rawScore = rawScore,
                smoothScore = smoothScore,
                elbowAngleL = feat.elbowL,
                elbowAngleR = feat.elbowR,
                stableMs = nowMs - stateSinceMs
            )
        )
    }

    // =========================
    // FEATURES
    // =========================

    private data class Features(
        val isReliable: Boolean,
        val upScore: Float,
        val elbowL: Float,
        val elbowR: Float
    )

    private fun computeFeatures(lm: List<NormalizedLandmark>): Features {
        // indices MediaPipe
        val lShoulder = lm[11]
        val rShoulder = lm[12]
        val lElbow = lm[13]
        val rElbow = lm[14]
        val lWrist = lm[15]
        val rWrist = lm[16]

        val visOk =
            visibilityOf(lShoulder) >= minVisibility &&
                    visibilityOf(rShoulder) >= minVisibility &&
                    visibilityOf(lElbow) >= minVisibility &&
                    visibilityOf(rElbow) >= minVisibility &&
                    visibilityOf(lWrist) >= minVisibility &&
                    visibilityOf(rWrist) >= minVisibility

        if (!visOk) {
            return Features(false, lastSmoothScore, lastElbowL, lastElbowR)
        }

        val elbowAngleL = angleDeg(lShoulder, lElbow, lWrist) // 180 arriba
        val elbowAngleR = angleDeg(rShoulder, rElbow, rWrist)
        val elbowAvg = (elbowAngleL + elbowAngleR) / 2f

        // Normaliza: 170° -> 1 (arriba), 75° -> 0 (abajo)
        val elbowUp = normalize(elbowAvg, minVal = 75f, maxVal = 170f)

        return Features(
            isReliable = true,
            upScore = elbowUp.coerceIn(0f, 1f),
            elbowL = elbowAngleL,
            elbowR = elbowAngleR
        )
    }

    // =========================
    // HELPERS
    // =========================

    private fun smooth(v: Float): Float {
        scoreBuffer.addLast(v)
        while (scoreBuffer.size > scoreWindowSize) scoreBuffer.removeFirst()
        val avg = scoreBuffer.sum() / scoreBuffer.size.toFloat()
        return avg.coerceIn(0f, 1f)
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

    private fun angleDeg(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Float {
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