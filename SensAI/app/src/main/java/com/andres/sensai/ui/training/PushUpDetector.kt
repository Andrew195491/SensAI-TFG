package com.andres.sensai.ui.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PushUpDetector(
    private val upThreshold: Float = 0.78f,
    private val downThreshold: Float = 0.28f,
    private val stableUpMs: Long = 90L,
    private val stableDownMs: Long = 70L,
    private val repCooldownMs: Long = 200L,
    private val minVisibility: Float = 0.35f,
    private val emaAlpha: Float = 0.38f,
    private val minVelocity: Float = 0.015f,
    private val maxAngleJump: Float = 30f,
    private val minDepthScore: Float = 0.32f,
    private val minTopScore: Float = 0.74f
) {

    enum class State {
        WAIT_UP,
        GOING_DOWN,
        WAIT_DOWN,
        GOING_UP
    }

    data class Debug(
        val rawScore: Float,
        val smoothScore: Float,
        val velocity: Float,
        val elbowAngleL: Float,
        val elbowAngleR: Float,
        val usedElbowAngle: Float,
        val bodyLineScore: Float,
        val stableMs: Long,
        val cycleMinScore: Float,
        val cycleMaxScore: Float
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

    private var smoothedScore = 1f
    private var prevSmoothScore = 1f

    private var cycleMinScore = 1f
    private var cycleMaxScore = 1f

    private var lastRawScore = 1f
    private var lastSmoothScore = 1f
    private var lastVelocity = 0f
    private var lastElbowL = 180f
    private var lastElbowR = 180f
    private var lastUsedElbow = 180f
    private var lastBodyLineScore = 1f

    fun reset(nowMs: Long = 0L) {
        state = State.WAIT_UP
        reps = 0
        stateSinceMs = nowMs
        lastRepAtMs = 0L

        smoothedScore = 1f
        prevSmoothScore = 1f

        cycleMinScore = 1f
        cycleMaxScore = 1f

        lastRawScore = 1f
        lastSmoothScore = 1f
        lastVelocity = 0f
        lastElbowL = 180f
        lastElbowR = 180f
        lastUsedElbow = 180f
        lastBodyLineScore = 1f
    }

    fun update(landmarks: List<NormalizedLandmark>, nowMs: Long): Output {
        if (stateSinceMs == 0L) stateSinceMs = nowMs

        if (landmarks.size < 33) {
            return buildOutput(nowMs)
        }

        val feat = computeFeatures(landmarks)
        if (!feat.isReliable) {
            return buildOutput(nowMs)
        }

        if (abs(feat.usedElbow - lastUsedElbow) > maxAngleJump) {
            return buildOutput(nowMs)
        }

        val rawScore = feat.upScore
        val smoothScore = smooth(rawScore)
        val velocity = smoothScore - prevSmoothScore
        prevSmoothScore = smoothScore

        val stableMs = nowMs - stateSinceMs
        val cooldownOk = (nowMs - lastRepAtMs) >= repCooldownMs

        when (state) {
            State.WAIT_UP -> {
                cycleMaxScore = max(cycleMaxScore, smoothScore)

                if (smoothScore < upThreshold && velocity < -minVelocity) {
                    state = State.GOING_DOWN
                    stateSinceMs = nowMs
                    cycleMinScore = smoothScore
                }
            }

            State.GOING_DOWN -> {
                cycleMinScore = min(cycleMinScore, smoothScore)

                if (smoothScore >= upThreshold && velocity > minVelocity) {
                    state = State.WAIT_UP
                    stateSinceMs = nowMs
                    cycleMaxScore = max(cycleMaxScore, smoothScore)
                } else if (
                    smoothScore <= downThreshold &&
                    stableMs >= stableDownMs &&
                    velocity <= 0.01f
                ) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs
                }
            }

            State.WAIT_DOWN -> {
                cycleMinScore = min(cycleMinScore, smoothScore)

                if (smoothScore > downThreshold && velocity > minVelocity) {
                    state = State.GOING_UP
                    stateSinceMs = nowMs
                    cycleMaxScore = smoothScore
                }
            }

            State.GOING_UP -> {
                cycleMaxScore = max(cycleMaxScore, smoothScore)

                if (smoothScore <= downThreshold && velocity < -minVelocity) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs
                } else if (
                    smoothScore >= upThreshold &&
                    stableMs >= stableUpMs &&
                    cooldownOk &&
                    cycleMinScore <= minDepthScore &&
                    cycleMaxScore >= minTopScore
                ) {
                    reps += 1
                    lastRepAtMs = nowMs
                    state = State.WAIT_UP
                    stateSinceMs = nowMs

                    cycleMinScore = 1f
                    cycleMaxScore = smoothScore
                }
            }
        }

        lastRawScore = rawScore
        lastSmoothScore = smoothScore
        lastVelocity = velocity
        lastElbowL = feat.elbowL
        lastElbowR = feat.elbowR
        lastUsedElbow = feat.usedElbow
        lastBodyLineScore = feat.bodyLineScore

        return buildOutput(nowMs)
    }

    private data class Features(
        val isReliable: Boolean,
        val upScore: Float,
        val elbowL: Float,
        val elbowR: Float,
        val usedElbow: Float,
        val bodyLineScore: Float
    )

    private fun computeFeatures(lm: List<NormalizedLandmark>): Features {
        val lShoulder = lm[11]
        val rShoulder = lm[12]
        val lElbow = lm[13]
        val rElbow = lm[14]
        val lWrist = lm[15]
        val rWrist = lm[16]
        val lHip = lm[23]
        val rHip = lm[24]
        val lAnkle = lm[27]
        val rAnkle = lm[28]

        val visLeft =
            visibilityOf(lShoulder) +
                    visibilityOf(lElbow) +
                    visibilityOf(lWrist)

        val visRight =
            visibilityOf(rShoulder) +
                    visibilityOf(rElbow) +
                    visibilityOf(rWrist)

        val leftOk =
            visibilityOf(lShoulder) >= minVisibility &&
                    visibilityOf(lElbow) >= minVisibility &&
                    visibilityOf(lWrist) >= minVisibility

        val rightOk =
            visibilityOf(rShoulder) >= minVisibility &&
                    visibilityOf(rElbow) >= minVisibility &&
                    visibilityOf(rWrist) >= minVisibility

        val bodyOk =
            visibilityOf(lHip) >= minVisibility &&
                    visibilityOf(rHip) >= minVisibility &&
                    visibilityOf(lAnkle) >= minVisibility &&
                    visibilityOf(rAnkle) >= minVisibility

        if (!leftOk && !rightOk) {
            return Features(
                isReliable = false,
                upScore = lastSmoothScore,
                elbowL = lastElbowL,
                elbowR = lastElbowR,
                usedElbow = lastUsedElbow,
                bodyLineScore = lastBodyLineScore
            )
        }

        val elbowAngleL = if (leftOk) angleDeg(lShoulder, lElbow, lWrist) else lastElbowL
        val elbowAngleR = if (rightOk) angleDeg(rShoulder, rElbow, rWrist) else lastElbowR

        val useLeft = when {
            leftOk && !rightOk -> true
            !leftOk && rightOk -> false
            else -> visLeft >= visRight
        }

        val usedElbow = if (useLeft) elbowAngleL else elbowAngleR

        val elbowUp = normalize(
            value = usedElbow,
            minVal = 75f,
            maxVal = 170f
        )

        val bodyLineScore = if (bodyOk) {
            val shoulder = avgPoint(lShoulder, rShoulder)
            val hip = avgPoint(lHip, rHip)
            val ankle = avgPoint(lAnkle, rAnkle)

            val bodyAngle = angleDeg(shoulder, hip, ankle)
            normalize(
                value = bodyAngle,
                minVal = 150f,
                maxVal = 180f
            )
        } else {
            1f
        }

        val upScore = (
                0.85f * elbowUp +
                        0.15f * bodyLineScore
                ).coerceIn(0f, 1f)

        return Features(
            isReliable = true,
            upScore = upScore,
            elbowL = elbowAngleL,
            elbowR = elbowAngleR,
            usedElbow = usedElbow,
            bodyLineScore = bodyLineScore
        )
    }

    private fun buildOutput(nowMs: Long): Output {
        return Output(
            reps = reps,
            state = state,
            score = lastSmoothScore,
            debug = Debug(
                rawScore = lastRawScore,
                smoothScore = lastSmoothScore,
                velocity = lastVelocity,
                elbowAngleL = lastElbowL,
                elbowAngleR = lastElbowR,
                usedElbowAngle = lastUsedElbow,
                bodyLineScore = lastBodyLineScore,
                stableMs = nowMs - stateSinceMs,
                cycleMinScore = cycleMinScore,
                cycleMaxScore = cycleMaxScore
            )
        )
    }

    private fun smooth(v: Float): Float {
        smoothedScore = emaAlpha * v + (1f - emaAlpha) * smoothedScore
        return smoothedScore.coerceIn(0f, 1f)
    }

    private data class P(val x: Float, val y: Float)

    private fun avgPoint(a: NormalizedLandmark, b: NormalizedLandmark): P {
        return P(
            x = (a.x() + b.x()) / 2f,
            y = (a.y() + b.y()) / 2f
        )
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

    private fun angleDeg(a: P, b: P, c: P): Float {
        val abx = a.x - b.x
        val aby = a.y - b.y
        val cbx = c.x - b.x
        val cby = c.y - b.y

        val dot = abx * cbx + aby * cby
        val abNorm = sqrt(abx * abx + aby * aby).coerceAtLeast(1e-6f)
        val cbNorm = sqrt(cbx * cbx + cby * cby).coerceAtLeast(1e-6f)

        val cosTheta = (dot / (abNorm * cbNorm)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosTheta).toDouble()).toFloat()
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
        return Math.toDegrees(acos(cosTheta).toDouble()).toFloat()
    }
}