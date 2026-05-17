package com.andres.sensai.ui.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SquatDetector(
    private val upThreshold: Float = 0.72f,
    private val downThreshold: Float = 0.42f,
    private val stableUpMs: Long = 90L,
    private val stableDownMs: Long = 90L,
    private val repCooldownMs: Long = 180L,
    private val minVisibility: Float = 0.35f,
    private val emaAlpha: Float = 0.40f,
    private val minVelocity: Float = 0.010f,
    private val maxAngleJump: Float = 35f,
    private val minDepthScore: Float = 0.46f,
    private val minTopScore: Float = 0.68f
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
        val metric: Float,
        val velocity: Float,
        val leftKneeAngle: Float,
        val rightKneeAngle: Float,
        val hipY: Float,
        val kneeY: Float,
        val ankleY: Float,
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
    private var lastMetric = 1f
    private var lastVelocity = 0f
    private var lastLeftKneeAngle = 180f
    private var lastRightKneeAngle = 180f
    private var lastHipY = 0f
    private var lastKneeY = 0f
    private var lastAnkleY = 0f
    private var lastKneeAngleAvg = 180f

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
        lastMetric = 1f
        lastVelocity = 0f
        lastLeftKneeAngle = 180f
        lastRightKneeAngle = 180f
        lastHipY = 0f
        lastKneeY = 0f
        lastAnkleY = 0f
        lastKneeAngleAvg = 180f
    }

    fun update(
        landmarks: List<NormalizedLandmark>,
        nowMs: Long
    ): Output {
        if (stateSinceMs == 0L) stateSinceMs = nowMs

        if (landmarks.size < 33) {
            return buildOutput(nowMs)
        }

        val feat = computeFeatures(landmarks)
        if (!feat.isReliable) {
            return buildOutput(nowMs)
        }

        val kneeAngleAvg = (feat.leftKneeAngle + feat.rightKneeAngle) / 2f
        if (abs(kneeAngleAvg - lastKneeAngleAvg) > maxAngleJump) {
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
                    stableMs >= stableDownMs
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
        lastMetric = feat.metric
        lastVelocity = velocity
        lastLeftKneeAngle = feat.leftKneeAngle
        lastRightKneeAngle = feat.rightKneeAngle
        lastHipY = feat.hipY
        lastKneeY = feat.kneeY
        lastAnkleY = feat.ankleY
        lastKneeAngleAvg = kneeAngleAvg

        return buildOutput(nowMs)
    }

    private data class Features(
        val isReliable: Boolean,
        val upScore: Float,
        val metric: Float,
        val leftKneeAngle: Float,
        val rightKneeAngle: Float,
        val hipY: Float,
        val kneeY: Float,
        val ankleY: Float
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

        if (!visOk) {
            return Features(
                isReliable = false,
                upScore = lastSmoothScore,
                metric = lastMetric,
                leftKneeAngle = lastLeftKneeAngle,
                rightKneeAngle = lastRightKneeAngle,
                hipY = lastHipY,
                kneeY = lastKneeY,
                ankleY = lastAnkleY
            )
        }

        val hip = avgPoint(lHip, rHip)
        val knee = avgPoint(lKnee, rKnee)
        val ankle = avgPoint(lAnkle, rAnkle)
        val shoulder = avgPoint(lShoulder, rShoulder)

        val leftKneeAngle = angleDeg(lHip, lKnee, lAnkle)
        val rightKneeAngle = angleDeg(rHip, rKnee, rAnkle)
        val kneeAngleAvg = (leftKneeAngle + rightKneeAngle) / 2f

        // Frontal/semi frontal: rodillas más permisivas
        val kneeMetricUp = normalize(
            value = kneeAngleAvg,
            minVal = 100f,
            maxVal = 172f
        )

        val torsoLen = distance(shoulder.x, shoulder.y, hip.x, hip.y).coerceAtLeast(1e-5f)

        // Diferencia vertical cadera-rodilla: arriba grande, abajo pequeña
        val hipToKneeY = (knee.y - hip.y)
        val verticalMetricUp = normalize(
            value = hipToKneeY / torsoLen,
            minVal = 0.08f,
            maxVal = 0.58f
        )

        // Cadera respecto al tobillo: arriba más alta, abajo menos
        val hipToAnkleY = (ankle.y - hip.y)
        val depthMetricUp = normalize(
            value = hipToAnkleY / torsoLen,
            minVal = 0.55f,
            maxVal = 1.45f
        )

        val metricUp =
            (0.60f * kneeMetricUp) +
                    (0.25f * verticalMetricUp) +
                    (0.15f * depthMetricUp)

        return Features(
            isReliable = true,
            upScore = metricUp.coerceIn(0f, 1f),
            metric = metricUp.coerceIn(0f, 1f),
            leftKneeAngle = leftKneeAngle,
            rightKneeAngle = rightKneeAngle,
            hipY = hip.y,
            kneeY = knee.y,
            ankleY = ankle.y
        )
    }

    private fun buildOutput(nowMs: Long): Output {
        val debug = Debug(
            rawScore = lastRawScore,
            smoothScore = lastSmoothScore,
            metric = lastMetric,
            velocity = lastVelocity,
            leftKneeAngle = lastLeftKneeAngle,
            rightKneeAngle = lastRightKneeAngle,
            hipY = lastHipY,
            kneeY = lastKneeY,
            ankleY = lastAnkleY,
            stableMs = nowMs - stateSinceMs,
            cycleMinScore = cycleMinScore,
            cycleMaxScore = cycleMaxScore
        )

        return Output(
            reps = reps,
            state = state,
            score = lastSmoothScore,
            debug = debug
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

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
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