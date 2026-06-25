package com.andres.sensai.ui.tutorial

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

class PushUpTutorialValidator(

    /*
     * Tiempo necesario para mantener correctamente
     * las posiciones de inicio y subida.
     */
    private val stableRequiredMs: Long = 400L,

    /*
     * En la posición inferior se exige menos tiempo
     * porque es una postura más difícil de mantener.
     */
    private val bottomStableRequiredMs: Long = 250L,

    /*
     * Tiempo durante el que se tolera un frame incorrecto
     * sin reiniciar inmediatamente el progreso.
     */
    private val incorrectGraceMs: Long = 150L,

    /*
     * Visibilidad mínima de los landmarks.
     *
     * Se ha reducido ligeramente porque en el suelo
     * algunas articulaciones pueden quedar ocultas.
     */
    private val minVisibility: Float = 0.22f,

    /*
     * Ángulo máximo respecto a la horizontal.
     *
     * 0°  = completamente horizontal.
     * 90° = completamente vertical.
     *
     * Una persona contra la pared seguirá siendo rechazada,
     * pero se permiten pequeñas inclinaciones en el suelo.
     */
    private val maxHorizontalAngle: Float = 42f

) : TutorialValidator {

    private var phase: TutorialPhase = TutorialPhase.READY

    /*
     * Momento en el que comenzó a mantenerse
     * correctamente la postura actual.
     */
    private var correctSinceMs: Long = 0L

    /*
     * Momento en el que comenzó una lectura incorrecta.
     *
     * Permite diferenciar un error puntual de MediaPipe
     * de una postura realmente incorrecta.
     */
    private var incorrectSinceMs: Long = 0L

    override fun reset() {
        phase = TutorialPhase.READY
        correctSinceMs = 0L
        incorrectSinceMs = 0L
    }

    override fun update(
        landmarks: List<NormalizedLandmark>,
        nowMs: Long
    ): TutorialFeedback {

        /*
         * El tutorial ya ha terminado.
         */
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

        /*
         * MediaPipe Pose debería proporcionar 33 landmarks.
         *
         * Si faltan, se considera una lectura incorrecta,
         * pero se aplica el margen de gracia.
         */
        if (landmarks.size < 33) {
            val stableMs = updateStableTime(
                correct = false,
                nowMs = nowMs
            )

            return feedback(
                phase = phase,
                correct = false,
                stableMs = stableMs,
                correction = "Deja visible todo el cuerpo."
            )
        }

        val features = computeFeatures(landmarks)

        /*
         * Si no se ven correctamente las articulaciones,
         * se aplica el margen de gracia antes de reiniciar
         * el progreso acumulado.
         */
        if (!features.isReliable) {
            val stableMs = updateStableTime(
                correct = false,
                nowMs = nowMs
            )

            return feedback(
                phase = phase,
                correct = false,
                stableMs = stableMs,
                correction = "Ponte de perfil y deja visibles hombro, codo, muñeca, cadera y tobillo."
            )
        }

        /*
         * Evita completar el tutorial haciendo flexiones
         * contra una pared o estando de pie.
         */
        if (!features.isHorizontal) {
            val stableMs = updateStableTime(
                correct = false,
                nowMs = nowMs
            )

            return feedback(
                phase = phase,
                correct = false,
                stableMs = stableMs,
                correction = "Colócate en el suelo, de perfil y con el cuerpo en horizontal."
            )
        }

        /*
         * La posición inferior permite una alineación
         * algo menos estricta.
         *
         * Al bajar, MediaPipe puede medir peor la cadera
         * o el tobillo por las oclusiones.
         */
        val bodyAligned = when (phase) {
            TutorialPhase.BOTTOM ->
                features.bodyLine >= 0.40f

            else ->
                features.bodyLine >= 0.48f
        }

        /*
         * Comprueba si la postura corresponde a la fase actual.
         */
        val isCorrect = when (phase) {

            /*
             * Posición superior.
             *
             * Antes se exigían 145°.
             * Ahora se permiten 142° para tolerar la perspectiva.
             */
            TutorialPhase.READY ->
                features.elbowAngle >= 142f &&
                        bodyAligned

            /*
             * Bajada intermedia.
             *
             * Se amplía ligeramente el rango válido.
             */
            TutorialPhase.DOWN ->
                features.elbowAngle in 108f..152f &&
                        bodyAligned

            /*
             * Posición inferior.
             *
             * Antes se exigía un ángulo menor o igual a 112°.
             * Ahora se permiten hasta 120°.
             */
            TutorialPhase.BOTTOM ->
                features.elbowAngle <= 120f &&
                        bodyAligned

            /*
             * Regreso a la posición superior.
             */
            TutorialPhase.UP ->
                features.elbowAngle >= 142f &&
                        bodyAligned

            TutorialPhase.DONE ->
                true
        }

        /*
         * Actualiza el tiempo durante el que la postura
         * se ha mantenido correctamente.
         */
        val stableMs = updateStableTime(
            correct = isCorrect,
            nowMs = nowMs
        )

        /*
         * Cada fase puede requerir un tiempo diferente.
         */
        val requiredStableMs = when (phase) {

            /*
             * La posición inferior solo necesita mantenerse
             * durante 250 ms.
             */
            TutorialPhase.BOTTOM ->
                bottomStableRequiredMs

            /*
             * La bajada es una fase de transición,
             * por lo que tampoco necesita 400 ms.
             */
            TutorialPhase.DOWN ->
                250L

            /*
             * Inicio y subida requieren 400 ms.
             */
            TutorialPhase.READY,
            TutorialPhase.UP ->
                stableRequiredMs

            TutorialPhase.DONE ->
                stableRequiredMs
        }

        /*
         * Avanza a la siguiente fase cuando la postura
         * se mantiene el tiempo requerido.
         */
        if (
            isCorrect &&
            stableMs >= requiredStableMs
        ) {
            phase = nextPhase(phase)
            resetStability()

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
            correction = correctionForPhase(
                phase = phase,
                features = features
            )
        )
    }

    private fun nextPhase(
        current: TutorialPhase
    ): TutorialPhase {
        return when (current) {
            TutorialPhase.READY ->
                TutorialPhase.DOWN

            TutorialPhase.DOWN ->
                TutorialPhase.BOTTOM

            TutorialPhase.BOTTOM ->
                TutorialPhase.UP

            TutorialPhase.UP ->
                TutorialPhase.DONE

            TutorialPhase.DONE ->
                TutorialPhase.DONE
        }
    }

    /*
     * Controla el tiempo estable y permite errores breves
     * sin reiniciar inmediatamente el progreso.
     */
    private fun updateStableTime(
        correct: Boolean,
        nowMs: Long
    ): Long {

        /*
         * Frame correcto.
         */
        if (correct) {
            incorrectSinceMs = 0L

            /*
             * Empieza a contar la estabilidad.
             */
            if (correctSinceMs == 0L) {
                correctSinceMs = nowMs
                return 0L
            }

            return nowMs - correctSinceMs
        }

        /*
         * Primer frame incorrecto.
         */
        if (incorrectSinceMs == 0L) {
            incorrectSinceMs = nowMs
        }

        val incorrectDuration =
            nowMs - incorrectSinceMs

        /*
         * Durante el margen de gracia no se elimina
         * el progreso acumulado.
         *
         * Esto tolera pequeños errores u oscilaciones
         * de MediaPipe.
         */
        if (incorrectDuration <= incorrectGraceMs) {
            return if (correctSinceMs == 0L) {
                0L
            } else {
                nowMs - correctSinceMs
            }
        }

        /*
         * La postura ha permanecido incorrecta durante
         * demasiado tiempo.
         *
         * Se reinicia el progreso.
         */
        correctSinceMs = 0L
        incorrectSinceMs = 0L

        return 0L
    }

    private fun resetStability() {
        correctSinceMs = 0L
        incorrectSinceMs = 0L
    }

    private fun feedback(
        phase: TutorialPhase,
        correct: Boolean,
        stableMs: Long,
        correction: String
    ): TutorialFeedback {

        return when (phase) {

            TutorialPhase.READY ->
                TutorialFeedback(
                    phase = phase,
                    isCorrect = correct,
                    title = "Fase 1 · Inicio",
                    instruction = "Colócate en horizontal con los brazos extendidos.",
                    correction = correction,
                    stableMs = stableMs
                )

            TutorialPhase.DOWN ->
                TutorialFeedback(
                    phase = phase,
                    isCorrect = correct,
                    title = "Fase 2 · Bajada",
                    instruction = "Flexiona los codos y baja con control.",
                    correction = correction,
                    stableMs = stableMs
                )

            TutorialPhase.BOTTOM ->
                TutorialFeedback(
                    phase = phase,
                    isCorrect = correct,
                    title = "Fase 3 · Abajo",
                    instruction = "Mantén brevemente la posición inferior.",
                    correction = correction,
                    stableMs = stableMs
                )

            TutorialPhase.UP ->
                TutorialFeedback(
                    phase = phase,
                    isCorrect = correct,
                    title = "Fase 4 · Subida",
                    instruction = "Empuja hasta volver arriba.",
                    correction = correction,
                    stableMs = stableMs
                )

            TutorialPhase.DONE ->
                TutorialFeedback(
                    phase = phase,
                    isCorrect = true,
                    title = "Tutorial completado",
                    instruction = "Movimiento correcto.",
                    correction = "",
                    stableMs = stableMs
                )
        }
    }

    private fun correctionForPhase(
        phase: TutorialPhase,
        features: Features
    ): String {

        if (!features.isHorizontal) {
            return "Colócate en el suelo con el cuerpo en horizontal."
        }

        /*
         * En la posición inferior se permite una alineación
         * mínima de 0.40.
         */
        val minimumBodyLine = when (phase) {
            TutorialPhase.BOTTOM -> 0.40f
            else -> 0.48f
        }

        if (features.bodyLine < minimumBodyLine) {
            return "Mantén hombros, cadera y tobillos alineados."
        }

        return when (phase) {

            TutorialPhase.READY ->
                "Extiende un poco más los brazos."

            TutorialPhase.DOWN ->
                "Flexiona los codos y continúa bajando."

            TutorialPhase.BOTTOM ->
                "Baja un poco más y mantén brevemente la posición."

            TutorialPhase.UP ->
                "Empuja hasta extender los brazos."

            TutorialPhase.DONE ->
                ""
        }
    }

    private data class Features(
        val isReliable: Boolean,
        val isHorizontal: Boolean,
        val elbowAngle: Float,
        val bodyLine: Float,
        val angleFromHorizontal: Float,
        val horizontalScore: Float
    )

    private fun computeFeatures(
        landmarks: List<NormalizedLandmark>
    ): Features {

        /*
         * Brazo izquierdo.
         */
        val leftShoulder = landmarks[11]
        val leftElbow = landmarks[13]
        val leftWrist = landmarks[15]

        /*
         * Brazo derecho.
         */
        val rightShoulder = landmarks[12]
        val rightElbow = landmarks[14]
        val rightWrist = landmarks[16]

        /*
         * Cuerpo.
         */
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]

        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]

        /*
         * Comprueba la visibilidad de cada brazo.
         */
        val leftArmOk =
            visibilityOf(leftShoulder) >= minVisibility &&
                    visibilityOf(leftElbow) >= minVisibility &&
                    visibilityOf(leftWrist) >= minVisibility

        val rightArmOk =
            visibilityOf(rightShoulder) >= minVisibility &&
                    visibilityOf(rightElbow) >= minVisibility &&
                    visibilityOf(rightWrist) >= minVisibility

        /*
         * Es suficiente con que uno de los dos brazos
         * sea visible.
         */
        if (!leftArmOk && !rightArmOk) {
            return invalidFeatures()
        }

        /*
         * Suma de visibilidad para escoger el mejor brazo.
         */
        val leftArmVisibility =
            visibilityOf(leftShoulder) +
                    visibilityOf(leftElbow) +
                    visibilityOf(leftWrist)

        val rightArmVisibility =
            visibilityOf(rightShoulder) +
                    visibilityOf(rightElbow) +
                    visibilityOf(rightWrist)

        val useLeft = when {
            leftArmOk && !rightArmOk ->
                true

            !leftArmOk && rightArmOk ->
                false

            else ->
                leftArmVisibility >= rightArmVisibility
        }

        /*
         * Utiliza hombro, codo, muñeca, cadera y tobillo
         * del mismo lado.
         */
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

        /*
         * Para cadera y tobillo se utiliza un umbral
         * ligeramente inferior.
         *
         * Estas articulaciones se utilizan para comprobar
         * la horizontalidad, pero pueden quedar parcialmente
         * ocultas durante la posición inferior.
         */
        val bodyVisibilityThreshold =
            minVisibility * 0.75f

        val selectedBodyOk =
            visibilityOf(shoulder) >= minVisibility &&
                    visibilityOf(hip) >= bodyVisibilityThreshold &&
                    visibilityOf(ankle) >= bodyVisibilityThreshold

        if (!selectedBodyOk) {
            return invalidFeatures()
        }

        /*
         * Ángulo del codo:
         *
         * hombro → codo → muñeca
         */
        val elbowAngle = angleDeg(
            shoulder,
            elbow,
            wrist
        )

        /*
         * Alineación corporal:
         *
         * hombro → cadera → tobillo
         */
        val hipAngle = angleDeg(
            shoulder,
            hip,
            ankle
        )

        /*
         * Se ha ampliado el rango inferior desde 145° hasta 140°.
         *
         * Esto evita que pequeñas oscilaciones de la cadera
         * invaliden continuamente la posición inferior.
         */
        val bodyLine = normalize(
            value = hipAngle,
            minVal = 140f,
            maxVal = 180f
        )

        /*
         * Calcula la orientación del cuerpo.
         */
        val deltaX =
            ankle.x() - shoulder.x()

        val deltaY =
            ankle.y() - shoulder.y()

        /*
         * 0°  = horizontal.
         * 90° = vertical.
         */
        val angleFromHorizontal =
            Math.toDegrees(
                atan2(
                    abs(deltaY),
                    abs(deltaX)
                ).toDouble()
            ).toFloat()

        /*
         * Puntuación para depuración.
         *
         * Hasta 20° → 1.
         * Desde 50° → 0.
         */
        val horizontalScore =
            1f - normalize(
                value = angleFromHorizontal,
                minVal = 20f,
                maxVal = 50f
            )

        val isHorizontal =
            angleFromHorizontal <= maxHorizontalAngle

        return Features(
            isReliable = true,
            isHorizontal = isHorizontal,
            elbowAngle = elbowAngle,
            bodyLine = bodyLine,
            angleFromHorizontal = angleFromHorizontal,
            horizontalScore = horizontalScore
        )
    }

    private fun invalidFeatures(): Features {
        return Features(
            isReliable = false,
            isHorizontal = false,
            elbowAngle = 180f,
            bodyLine = 0f,
            angleFromHorizontal = 90f,
            horizontalScore = 0f
        )
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

    /*
     * Calcula el ángulo formado por tres landmarks.
     *
     * El segundo landmark es el vértice.
     */
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