package com.andres.sensai.ui.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PushUpDetector(

    /*
     * Score necesario para considerar que el usuario
     * está en la posición superior.
     *
     * Con la normalización 75°-170°:
     * 0.70 equivale aproximadamente a 142°.
     */
    private val upThreshold: Float = 0.70f,

    /*
     * Score necesario para considerar que el usuario
     * ha llegado a la posición inferior.
     *
     * 0.42 equivale aproximadamente a 115°.
     */
    private val downThreshold: Float = 0.42f,

    private val stableUpMs: Long = 70L,
    private val stableDownMs: Long = 50L,
    private val repCooldownMs: Long = 220L,

    /*
     * MediaPipe puede perder parcialmente una articulación
     * durante una flexión en suelo.
     */
    private val minVisibility: Float = 0.20f,

    /*
     * Respuesta relativamente rápida sin eliminar
     * completamente el suavizado.
     */
    private val emaAlpha: Float = 0.55f,

    /*
     * Se conserva por compatibilidad y depuración.
     *
     * Ya no bloquea por sí sola las transiciones.
     */
    private val minVelocity: Float = 0.004f,

    /*
     * Si hay un salto superior, se recalibra la referencia
     * en lugar de bloquear continuamente el detector.
     */
    private val maxAngleJump: Float = 65f,

    private val minDepthScore: Float = 0.45f,
    private val minTopScore: Float = 0.65f,

    /*
     * Una persona contra la pared suele acercarse a 90°.
     * Una flexión real puede inclinarse algo por perspectiva.
     */
    private val maxHorizontalAngle: Float = 45f,

    /*
     * Tiempo durante el que se toleran landmarks inválidos
     * sin reiniciar la repetición.
     */
    private val trackingGraceMs: Long = 300L

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
        val horizontalScore: Float,
        val angleFromHorizontal: Float,
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

    private var smoothedScore: Float = 1f
    private var prevSmoothScore: Float = 1f

    private var cycleMinScore: Float = 1f
    private var cycleMaxScore: Float = 1f

    private var lastRawScore: Float = 1f
    private var lastSmoothScore: Float = 1f
    private var lastVelocity: Float = 0f

    private var lastElbowL: Float = 180f
    private var lastElbowR: Float = 180f
    private var lastUsedElbow: Float = 180f

    private var lastBodyLineScore: Float = 1f
    private var lastHorizontalScore: Float = 0f
    private var lastAngleFromHorizontal: Float = 90f

    /*
     * null  -> lado todavía no bloqueado
     * true  -> brazo izquierdo
     * false -> brazo derecho
     *
     * Durante una repetición se conserva el mismo lado.
     */
    private var lockedUseLeft: Boolean? = null

    /*
     * Control de pérdidas breves de seguimiento.
     */
    private var trackingLostSinceMs: Long = 0L
    private var needsRebaseline: Boolean = true

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
        lastHorizontalScore = 0f
        lastAngleFromHorizontal = 90f

        lockedUseLeft = null

        trackingLostSinceMs = 0L
        needsRebaseline = true
    }

    fun update(
        landmarks: List<NormalizedLandmark>,
        nowMs: Long
    ): Output {

        if (stateSinceMs == 0L) {
            stateSinceMs = nowMs
        }

        if (landmarks.size < 33) {
            handleTrackingLoss(nowMs)
            return buildOutput(nowMs)
        }

        val features = computeFeatures(landmarks)

        /*
         * Actualizamos siempre la información visual
         * para poder verla en Debug.
         */
        lastElbowL = features.elbowL
        lastElbowR = features.elbowR
        lastBodyLineScore = features.bodyLineScore
        lastHorizontalScore = features.horizontalScore
        lastAngleFromHorizontal = features.angleFromHorizontal

        /*
         * Si el frame no es válido, se mantiene el estado.
         * No se cancela inmediatamente la repetición.
         */
        if (!features.isReliable) {
            handleTrackingLoss(nowMs)
            return buildOutput(nowMs)
        }

        /*
         * El seguimiento ha vuelto a ser válido.
         */
        trackingLostSinceMs = 0L

        /*
         * Después de perder seguimiento se toma el primer
         * ángulo válido como nueva referencia.
         */
        if (needsRebaseline) {
            needsRebaseline = false

            lastUsedElbow = features.usedElbow

            val initialScore = normalize(
                value = features.usedElbow,
                minVal = 75f,
                maxVal = 170f
            )

            smoothedScore = initialScore
            prevSmoothScore = initialScore

            lastRawScore = initialScore
            lastSmoothScore = initialScore
            lastVelocity = 0f

            return buildOutput(nowMs)
        }

        val angleJump =
            abs(features.usedElbow - lastUsedElbow)

        /*
         * Antes el frame se rechazaba y la referencia antigua
         * permanecía sin actualizarse.
         *
         * Ahora se recalibra, evitando que el detector quede
         * bloqueado después de una oclusión.
         */
        if (angleJump > maxAngleJump) {
            lastUsedElbow = features.usedElbow

            val recalibratedScore = normalize(
                value = features.usedElbow,
                minVal = 75f,
                maxVal = 170f
            )

            smoothedScore = recalibratedScore
            prevSmoothScore = recalibratedScore

            lastRawScore = recalibratedScore
            lastSmoothScore = recalibratedScore
            lastVelocity = 0f

            return buildOutput(nowMs)
        }

        lastUsedElbow = features.usedElbow

        val rawScore = features.upScore
        val smoothScore = smooth(rawScore)

        val velocity =
            smoothScore - prevSmoothScore

        prevSmoothScore = smoothScore

        val stableMs =
            nowMs - stateSinceMs

        val cooldownOk =
            nowMs - lastRepAtMs >= repCooldownMs

        when (state) {

            State.WAIT_UP -> {
                /*
                 * En reposo se permite seleccionar el brazo
                 * que tenga mayor visibilidad.
                 */
                lockedUseLeft = null

                cycleMaxScore =
                    max(cycleMaxScore, smoothScore)

                /*
                 * Se inicia la bajada al abandonar claramente
                 * la posición superior.
                 *
                 * No exigimos una velocidad mínima alta.
                 */
                if (
                    smoothScore < upThreshold - 0.05f
                ) {
                    state = State.GOING_DOWN
                    stateSinceMs = nowMs

                    cycleMinScore = smoothScore
                    cycleMaxScore = max(
                        cycleMaxScore,
                        smoothScore
                    )

                    lockedUseLeft = features.useLeft
                }
            }

            State.GOING_DOWN -> {
                cycleMinScore =
                    min(cycleMinScore, smoothScore)

                /*
                 * Posición inferior.
                 *
                 * Se permite un pequeño margen para que no dependa
                 * de alcanzar un score exacto durante muchos frames.
                 */
                if (
                    smoothScore <= downThreshold &&
                    stableMs >= stableDownMs
                ) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs

                    /*
                     * Si vuelve arriba sin llegar abajo,
                     * se cancela el intento.
                     */
                } else if (
                    smoothScore >= upThreshold &&
                    stableMs >= stableUpMs
                ) {
                    state = State.WAIT_UP
                    stateSinceMs = nowMs

                    cycleMinScore = 1f
                    cycleMaxScore = smoothScore

                    lockedUseLeft = null
                }
            }

            State.WAIT_DOWN -> {
                cycleMinScore =
                    min(cycleMinScore, smoothScore)

                /*
                 * Empieza la subida al abandonar claramente
                 * la zona inferior.
                 */
                if (
                    smoothScore > downThreshold + 0.06f
                ) {
                    state = State.GOING_UP
                    stateSinceMs = nowMs
                    cycleMaxScore = smoothScore
                }
            }

            State.GOING_UP -> {
                cycleMaxScore =
                    max(cycleMaxScore, smoothScore)

                /*
                 * Si vuelve a bajar antes de completar,
                 * regresa al estado inferior.
                 */
                if (
                    smoothScore <= downThreshold &&
                    stableMs >= stableDownMs
                ) {
                    state = State.WAIT_DOWN
                    stateSinceMs = nowMs

                    /*
                     * Completa la repetición al regresar arriba.
                     */
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

                    lockedUseLeft = null
                }
            }
        }

        lastRawScore = rawScore
        lastSmoothScore = smoothScore
        lastVelocity = velocity

        lastElbowL = features.elbowL
        lastElbowR = features.elbowR
        lastUsedElbow = features.usedElbow

        lastBodyLineScore =
            features.bodyLineScore

        lastHorizontalScore =
            features.horizontalScore

        lastAngleFromHorizontal =
            features.angleFromHorizontal

        return buildOutput(nowMs)
    }

    /*
     * Tolera pérdidas breves de landmarks.
     */
    private fun handleTrackingLoss(
        nowMs: Long
    ) {
        if (trackingLostSinceMs == 0L) {
            trackingLostSinceMs = nowMs
        }

        /*
         * Después de una pérdida prolongada, el siguiente
         * frame válido se utilizará como nueva referencia.
         *
         * No se reinicia el contador de repeticiones.
         */
        if (
            nowMs - trackingLostSinceMs >
            trackingGraceMs
        ) {
            needsRebaseline = true
        }
    }

    private data class Features(
        val isReliable: Boolean,
        val useLeft: Boolean,
        val upScore: Float,
        val elbowL: Float,
        val elbowR: Float,
        val usedElbow: Float,
        val bodyLineScore: Float,
        val horizontalScore: Float,
        val angleFromHorizontal: Float
    )

    private fun computeFeatures(
        landmarks: List<NormalizedLandmark>
    ): Features {

        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]

        val leftElbow = landmarks[13]
        val rightElbow = landmarks[14]

        val leftWrist = landmarks[15]
        val rightWrist = landmarks[16]

        val leftHip = landmarks[23]
        val rightHip = landmarks[24]

        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]

        val leftArmOk =
            visibilityOf(leftShoulder) >= minVisibility &&
                    visibilityOf(leftElbow) >= minVisibility &&
                    visibilityOf(leftWrist) >= minVisibility

        val rightArmOk =
            visibilityOf(rightShoulder) >= minVisibility &&
                    visibilityOf(rightElbow) >= minVisibility &&
                    visibilityOf(rightWrist) >= minVisibility

        if (!leftArmOk && !rightArmOk) {
            return invalidFeatures()
        }

        val leftElbowAngle =
            if (leftArmOk) {
                angleDeg(
                    leftShoulder,
                    leftElbow,
                    leftWrist
                )
            } else {
                lastElbowL
            }

        val rightElbowAngle =
            if (rightArmOk) {
                angleDeg(
                    rightShoulder,
                    rightElbow,
                    rightWrist
                )
            } else {
                lastElbowR
            }

        val leftArmVisibility =
            visibilityOf(leftShoulder) +
                    visibilityOf(leftElbow) +
                    visibilityOf(leftWrist)

        val rightArmVisibility =
            visibilityOf(rightShoulder) +
                    visibilityOf(rightElbow) +
                    visibilityOf(rightWrist)

        /*
         * Durante una repetición se mantiene el brazo seleccionado.
         */
        val useLeft = when {

            lockedUseLeft == true && leftArmOk ->
                true

            lockedUseLeft == false && rightArmOk ->
                false

            leftArmOk && !rightArmOk ->
                true

            !leftArmOk && rightArmOk ->
                false

            else ->
                leftArmVisibility >= rightArmVisibility
        }

        val shoulder =
            if (useLeft) leftShoulder else rightShoulder

        val elbow =
            if (useLeft) leftElbow else rightElbow

        val wrist =
            if (useLeft) leftWrist else rightWrist

        val hip =
            if (useLeft) leftHip else rightHip

        val ankle =
            if (useLeft) leftAnkle else rightAnkle

        val usedElbowAngle =
            if (useLeft) {
                leftElbowAngle
            } else {
                rightElbowAngle
            }

        /*
         * La cadera y el tobillo solo se usan para comprobar
         * que el cuerpo está horizontal.
         *
         * Se les aplica un umbral algo más permisivo.
         */
        val bodyVisibilityThreshold =
            minVisibility * 0.70f

        val selectedBodyOk =
            visibilityOf(hip) >= bodyVisibilityThreshold &&
                    visibilityOf(ankle) >= bodyVisibilityThreshold

        if (!selectedBodyOk) {
            return Features(
                isReliable = false,
                useLeft = useLeft,
                upScore = lastSmoothScore,
                elbowL = leftElbowAngle,
                elbowR = rightElbowAngle,
                usedElbow = usedElbowAngle,
                bodyLineScore = lastBodyLineScore,
                horizontalScore = lastHorizontalScore,
                angleFromHorizontal = lastAngleFromHorizontal
            )
        }

        val elbowUpScore = normalize(
            value = usedElbowAngle,
            minVal = 75f,
            maxVal = 170f
        )

        val bodyAngle = angleDeg(
            shoulder,
            hip,
            ankle
        )

        val bodyLineScore = normalize(
            value = bodyAngle,
            minVal = 140f,
            maxVal = 180f
        )

        val deltaX =
            ankle.x() - shoulder.x()

        val deltaY =
            ankle.y() - shoulder.y()

        val angleFromHorizontal =
            Math.toDegrees(
                atan2(
                    abs(deltaY),
                    abs(deltaX)
                ).toDouble()
            ).toFloat()

        /*
         * Se usa principalmente para depuración.
         */
        val horizontalScore =
            1f - normalize(
                value = angleFromHorizontal,
                minVal = 20f,
                maxVal = 55f
            )

        /*
         * Conserva la limitación horizontal,
         * pero con algo más de tolerancia.
         */
        val horizontalOk =
            angleFromHorizontal <= maxHorizontalAngle

        return Features(
            isReliable = horizontalOk,
            useLeft = useLeft,
            upScore = elbowUpScore.coerceIn(0f, 1f),
            elbowL = leftElbowAngle,
            elbowR = rightElbowAngle,
            usedElbow = usedElbowAngle,
            bodyLineScore = bodyLineScore,
            horizontalScore = horizontalScore,
            angleFromHorizontal = angleFromHorizontal
        )
    }

    private fun invalidFeatures(): Features {
        return Features(
            isReliable = false,
            useLeft = lockedUseLeft ?: true,
            upScore = lastSmoothScore,
            elbowL = lastElbowL,
            elbowR = lastElbowR,
            usedElbow = lastUsedElbow,
            bodyLineScore = lastBodyLineScore,
            horizontalScore = lastHorizontalScore,
            angleFromHorizontal = lastAngleFromHorizontal
        )
    }

    private fun buildOutput(
        nowMs: Long
    ): Output {
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
                horizontalScore = lastHorizontalScore,
                angleFromHorizontal = lastAngleFromHorizontal,
                stableMs = nowMs - stateSinceMs,
                cycleMinScore = cycleMinScore,
                cycleMaxScore = cycleMaxScore
            )
        )
    }

    private fun smooth(
        value: Float
    ): Float {
        smoothedScore =
            emaAlpha * value +
                    (1f - emaAlpha) * smoothedScore

        return smoothedScore.coerceIn(0f, 1f)
    }

    private fun visibilityOf(
        landmark: NormalizedLandmark
    ): Float {
        return try {
            landmark.visibility().orElse(1f)
        } catch (_: Throwable) {
            1f
        }
    }

    private fun normalize(
        value: Float,
        minVal: Float,
        maxVal: Float
    ): Float {

        if (maxVal <= minVal) {
            return 0f
        }

        return (
                (value - minVal) /
                        (maxVal - minVal)
                ).coerceIn(0f, 1f)
    }

    private fun angleDeg(
        first: NormalizedLandmark,
        vertex: NormalizedLandmark,
        third: NormalizedLandmark
    ): Float {

        val firstX =
            first.x() - vertex.x()

        val firstY =
            first.y() - vertex.y()

        val thirdX =
            third.x() - vertex.x()

        val thirdY =
            third.y() - vertex.y()

        val dot =
            firstX * thirdX +
                    firstY * thirdY

        val firstNorm =
            sqrt(
                firstX * firstX +
                        firstY * firstY
            ).coerceAtLeast(1e-6f)

        val thirdNorm =
            sqrt(
                thirdX * thirdX +
                        thirdY * thirdY
            ).coerceAtLeast(1e-6f)

        val cosTheta =
            (dot / (firstNorm * thirdNorm))
                .coerceIn(-1f, 1f)

        return Math.toDegrees(
            acos(cosTheta.toDouble())
        ).toFloat()
    }
}