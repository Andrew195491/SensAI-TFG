package com.andres.sensai.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

@Composable
fun TutorialPoseOverlay(
    landmarks: List<NormalizedLandmark>,
    color: Color,
    mirror: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (landmarks.size < 33) return@Canvas

        val connections = listOf(
            11 to 12,
            11 to 13, 13 to 15,
            12 to 14, 14 to 16,
            11 to 23, 12 to 24,
            23 to 24,
            23 to 25, 25 to 27,
            24 to 26, 26 to 28
        )

        fun lmPoint(index: Int): Offset {
            val lm = landmarks[index]
            val x = if (mirror) 1f - lm.x() else lm.x()
            val y = lm.y()
            return Offset(x * size.width, y * size.height)
        }

        val stroke = size.minDimension * 0.008f
        val pointRadius = size.minDimension * 0.012f

        connections.forEach { (a, b) ->
            val pa = lmPoint(a)
            val pb = lmPoint(b)
            drawLine(
                color = color,
                start = pa,
                end = pb,
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        val visiblePoints = listOf(
            11, 12, 13, 14, 15, 16,
            23, 24, 25, 26, 27, 28
        )

        visiblePoints.forEach { index ->
            val p = lmPoint(index)
            drawCircle(
                color = color,
                radius = pointRadius,
                center = p
            )
        }
    }
}