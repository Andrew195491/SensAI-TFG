package com.andres.sensai.ui.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class SquatDetector(
    private val upThreshold: Float = 0.68f,
    private val downThreshold: Float = 0.42f,
    private val stableRequiredMs: Long = 180L,
    private val repCooldownMs: Long = 300L,
    private val scoreWindowSize: Int = 6,
    private val minVisibility: Float = 0.35f
) {

    enum class State {
        WAIT_UP,      // arriba estable
        GOING_DOWN,   // bajando
        WAIT_DOWN,    // abajo estable
        GOING_UP      // subiendo
    }

    data class Debug(
        val rawScore: Float,
        val smoothScore: Float,
        val metric: Float, // métrica base (0..1): 1 ~ arriba, 0 ~ abajo
        val leftKneeAngle: Float,
        val rightKneeAngle: Float,
        val hipY: Float,
        val kneeY: Float,
        val ankleY: Float,
        val stableMs: Long
    )

    data class Output(
        val reps: Int,
        val state: State,
        val score: Float,      // score suavizado (0..1)
        val debug: Debug
    )

    private var state: State = State.WAIT_UP
    private var reps: Int = 0

    private var stateSinceMs: Long = 0L
    private var lastRepAtMs: Long = 0L

    // para suavizar score
    private val scoreBuffer = ArrayDeque<Float>()

    // último debug válido
    private var lastRawScore = 1f
    private var lastSmoothScore = 1f
    private var lastMetric = 1f
    private var lastLeftKneeAngle = 180f
    private var lastRightKneeAngle = 180f
    private var lastHipY = 0f
    private var lastKneeY = 0f
    private var lastAnkleY = 0f

    fun reset(nowMs: Long = 0L) {
        state = State.WAIT_UP
        reps = 0
        stateSinceMs = nowMs
        lastRepAtMs = 0L
        scoreBuffer.clear()
    }

    fun update(
        landmarks: List<NormalizedLandmark>,
        nowMs: Long
    ): Output {
        if (stateSinceMs == 0L) stateSinceMs = nowMs

        // Si no hay landmarks suficientes, no avanzamos estado
        if (landmarks.size < 33) {
            val debug = Debug(
                rawScore = lastRawScore,
                smoothScore = lastSmoothScore,
                metric = lastMetric,
                leftKneeAngle = lastLeftKneeAngle,
                rightKneeAngle = lastRightKneeAngle,
                hipY = lastHipY,
                kneeY = lastKneeY,
                ankleY = lastAnkleY,
                stableMs = nowMs - stateSinceMs
            )
            return Output(reps, state, lastSmoothScore, debug)
        }

        // --- 1) Calcular features robustas ---
        val feat = computeFeatures(landmarks)

        // Si la visibilidad es mala, mantenemos estado y no metemos ruido
        if (!feat.isReliable) {
            val debug = Debug(
                rawScore = lastRawScore,
                smoothScore = lastSmoothScore,
                metric = lastMetric,
                leftKneeAngle = lastLeftKneeAngle,
                rightKneeAngle = lastRightKneeAngle,
                hipY = lastHipY,
                kneeY = lastKneeY,
                ankleY = lastAnkleY,
                stableMs = nowMs - stateSinceMs
            )
            return Output(reps, state, lastSmoothScore, debug)
        }

        // --- 2) Score bruto (0..1): 1=arriba, 0=abajo) ---
        val rawScore = feat.upScore

        // --- 3) Suavizado ---
        val smoothScore = smooth(rawScore)

        // --- 4) Máquina de estados ---
        val stableMs = nowMs - stateSinceMs
        val cooldownOk = (nowMs - lastRepAtMs) >= repCooldownMs

        when (state) {
            State.WAIT_UP -> {
                // Estamos arriba. Si empieza a bajar de verdad, cambiamos.
                if (smoothScore < upThreshold) {
                    state = State.GOING_DOWN
                    stateSinceMs = nowMs
                }
            }

            State.GOING_DOWN -> {
                // Si vuelve arriba, cancelamos bajada.
                if (smoothScore >= upThreshold) {
                    state = State.WAIT_UP
                    stateSinceMs = nowMs
                }
                // Si llega abajo y se mantiene estable, abajo confirmado.
                else if (smoothScore <= downThreshold && stableMs >= stableRequiredMs) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs
                }
            }

            State.WAIT_DOWN -> {
                // Estamos abajo. Si empieza a subir, cambiamos.
                if (smoothScore > downThreshold) {
                    state = State.GOING_UP
                    stateSinceMs = nowMs
                }
            }

            State.GOING_UP -> {
                // Si vuelve abajo, cancelamos subida.
                if (smoothScore <= downThreshold) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs
                }
                // Si llega arriba y se mantiene estable -> REP++
                else if (smoothScore >= upThreshold && stableMs >= stableRequiredMs && cooldownOk) {
                    reps += 1
                    lastRepAtMs = nowMs
                    state = State.WAIT_UP
                    stateSinceMs = nowMs
                }
            }
        }

        // Guardar últimos valores debug
        lastRawScore = rawScore
        lastSmoothScore = smoothScore
        lastMetric = feat.metric
        lastLeftKneeAngle = feat.leftKneeAngle
        lastRightKneeAngle = feat.rightKneeAngle
        lastHipY = feat.hipY
        lastKneeY = feat.kneeY
        lastAnkleY = feat.ankleY

        val debug = Debug(
            rawScore = rawScore,
            smoothScore = smoothScore,
            metric = feat.metric,
            leftKneeAngle = feat.leftKneeAngle,
            rightKneeAngle = feat.rightKneeAngle,
            hipY = feat.hipY,
            kneeY = feat.kneeY,
            ankleY = feat.ankleY,
            stableMs = nowMs - stateSinceMs
        )

        return Output(
            reps = reps,
            state = state,
            score = smoothScore,
            debug = debug
        )
    }

    // =========================
    // FEATURES
    // =========================

    private data class Features(
        val isReliable: Boolean,
        val upScore: Float,       // 1=arriba, 0=abajo
        val metric: Float,        // también 1=arriba, 0=abajo (antes de combinar)
        val leftKneeAngle: Float,
        val rightKneeAngle: Float,
        val hipY: Float,
        val kneeY: Float,
        val ankleY: Float
    )

    private fun computeFeatures(lm: List<NormalizedLandmark>): Features {
        // MediaPipe indices
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

        // Promedios izquierda/derecha (más robusto)
        val hip = avgPoint(lHip, rHip)
        val knee = avgPoint(lKnee, rKnee)
        val ankle = avgPoint(lAnkle, rAnkle)
        val shoulder = avgPoint(lShoulder, rShoulder)

        val leftKneeAngle = angleDeg(lHip, lKnee, lAnkle)   // ~180 arriba, menor al bajar
        val rightKneeAngle = angleDeg(rHip, rKnee, rAnkle)  // ~180 arriba, menor al bajar
        val kneeAngleAvg = (leftKneeAngle + rightKneeAngle) / 2f

        // Normalización del ángulo de rodilla:
        // 180° => muy arriba (1.0)
        // 90°  => muy abajo (0.0)
        val kneeMetricUp = normalize(
            value = kneeAngleAvg,
            minVal = 90f,
            maxVal = 175f
        )

        // Métrica de profundidad vertical relativa:
        // cuando bajas, la cadera se acerca más a la rodilla
        val torsoLen = distance(shoulder.x, shoulder.y, hip.x, hip.y).coerceAtLeast(1e-5f)
        val hipToKneeY = (knee.y - hip.y) // >0 normalmente
        // más grande => más "arriba"; más pequeño => más "abajo"
        val verticalMetricUp = normalize(
            value = hipToKneeY / torsoLen,
            minVal = 0.15f,
            maxVal = 0.70f
        )

        // Mezcla robusta (ángulo pesa más)
        val metricUp = (0.7f * kneeMetricUp) + (0.3f * verticalMetricUp)

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

    // =========================
    // HELPERS
    // =========================

    private fun smooth(v: Float): Float {
        scoreBuffer.addLast(v)
        while (scoreBuffer.size > scoreWindowSize) {
            scoreBuffer.removeFirst()
        }
        val avg = scoreBuffer.sum() / scoreBuffer.size.toFloat()
        return avg.coerceIn(0f, 1f)
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
            // según versión de MediaPipe, visibility puede no venir
            1f
        }
    }

    private fun normalize(value: Float, minVal: Float, maxVal: Float): Float {
        if (maxVal <= minVal) return 0f
        val t = (value - minVal) / (maxVal - minVal)
        return t.coerceIn(0f, 1f)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Ángulo ABC (en B), en grados.
     */
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